package com.bose.services.acms.client.aem;

import com.bose.services.acms.client.aem.impl.JcrSessionTemplate;
import com.bose.services.acms.client.aem.impl.ManagedConfiguration;
import com.bose.services.acms.client.aem.impl.ManagedConfigurationMixin;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Service for tracking managed configurations. Main API for programmatically adding and removing nodes from managed configuration,
 * and to refresh managed configuration.
 * <p>
 * Note that this service is going be called from multiple threads, so everything in here must be thread-safe and optimized
 * for concurrent access!
 *
 * @author Niki Driessen
 */
@Component(immediate = true, name = "ManagedConfigurationTracker")
@Service(ManagedConfigurationTracker.class)
public class ManagedConfigurationTracker {
    public static final String REFRESH_ALL = "*";
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationTracker.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    //NEVER use directly, always go through the getter to ensure proper synchronization in multi-threaded access...
    private Set<ManagedConfiguration> managedConfigurations = new HashSet<>();

    @Reference
    private ConfigurationProvider configurationProvider;
    @Reference
    private SlingRepository repository;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private JcrSessionTemplate<Void> sessionTemplate;

    /**
     * Starts tracking this node as a managed configuration node.
     * It places no constraints or conditions on a node to become managed.
     * However, in order to do it's work, the node will get the {@link ManagedConfigurationMixin#NODE_TYPE} mixin if it doesn't already has it.
     * The mixin will be used to track and manage meta-data, for more information see {@link ManagedConfiguration}.
     * <p>
     * This method is thread-safe and can be called concurrently, but it <strong>MUST</strong> be called from with an active {@link javax.jcr.Session}.
     *
     * @param node the node to be tracked, not null.
     * @return <code>true</code> if the node is now tracked, false if it was already tracked.
     * @throws ManagedConfigurationException when configuring the tracked node failed.
     * @throws RepositoryException           when not accessed from within an active session or when the node could not be accessed for some reason (e.g. deleted).
     * @see ManagedConfiguration
     */
    public boolean track(Node node) throws ManagedConfigurationException, RepositoryException {
        Assert.notNull(node);
        //logger.info("Tracking node '{}' for managed configuration changes.", nodePath);
        Resource resource = null;
        try {
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            resource = resourceResolver.getResource(node.getPath());
        } catch (LoginException e) {
            throw new RepositoryException("Error getting resource resolver", e);
        }
        ManagedConfiguration configuration = new ManagedConfiguration(resource);
        if (threadSafeWrite(configuration)) {
            try {
                configuration.configure(configurationProvider, sessionTemplate);
                logger.info("Successfully registered node '{}' for managed configuration tracking.", configuration.getNodePath());
                return true;
            } catch (IllegalStateException e) {
                /** this means the node couldn't be accessed by the configure run. This should only happen in race conditions,
                 * where the node is deleted in the time between the caller calling this method and the configure method accessing the node,
                 * this should be rare.
                 */
                untrack(configuration, e);
                throw new RepositoryException("Error starting tracking on node, not adding to track list.", e);
            }
        }//else: nothing was added (re-register of node), so don't configure again
        return false;
    }

    /**
     * Convenience method for {@link #untrack(ManagedConfiguration, Exception)} without an exception as a cause for untracking the node.
     *
     * @param configuration the managed configuration to untrack, not null.
     * @see #untrack(ManagedConfiguration, Exception)
     */
    @SuppressWarnings("unused")
    public void untrack(ManagedConfiguration configuration) {
        untrack(configuration, null);
    }

    /**
     * Convenience method for {@link #untrack(ManagedConfiguration, Exception)} without an exception as a cause for untracking the node.
     * This method is thread-safe and can be called concurrently, but it <strong>MUST</strong> be called from with an active {@link javax.jcr.Session}.
     *
     * @param path the path of the managed node to untrack, not null.
     * @throws RepositoryException when not called from within an active session, or when the node cannot be accessed.
     * @see #untrack(ManagedConfiguration, Exception)
     */
    @SuppressWarnings("unused")
    public void untrack(String path) throws RepositoryException {
        Assert.notNull(path);
        Set<ManagedConfiguration> configurations = getManagedConfigurations();
        for (ManagedConfiguration configuration : configurations) {
            if (configuration.isForNode(path)) {
                untrack(configuration);
                return;
            }
        }
    }

    /**
     * Removes the managed configuration from tracking. This can happen on a refresh event, if the node is not accessible anymore.
     * Typically, when the node has been removed in the meanwhile through some other process or manual action.
     * <p>
     * You can also call this method to explicitly untrack a configuration of course.
     *
     * @param configuration the managed configuration to untrack, not null.
     * @param cause         the exception that caused this call to untrack, if any, optional.
     */
    public void untrack(ManagedConfiguration configuration, Exception cause) {
        Assert.notNull(configuration);
        String message = String.format("Untracking node '%s' for managed configuration changes.", configuration.getNodePath());
        threadSafeWrite(configuration, true);
        if (cause == null) {
            logger.info(message);
        } else {
            logger.error(message, cause);
        }
    }

    protected boolean threadSafeWrite(ManagedConfiguration configuration) {
        return threadSafeWrite(configuration, false);
    }

    protected boolean threadSafeWrite(ManagedConfiguration configuration, boolean remove) {
        try {
            lock.writeLock().lock();
            return remove ? managedConfigurations.remove(configuration) : managedConfigurations.add(configuration);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the set of managed configurations.
     * <p>
     * You can use this to programmatically (re-)configure managed nodes, or to pass them to {@link #untrack(ManagedConfiguration)} e.g..
     * This method is thread safe and can be called concurrently.
     *
     * @return the set of managed configurations.
     */
    public Set<ManagedConfiguration> getManagedConfigurations() {
        try {
            lock.readLock().lock();
            return new HashSet<>(managedConfigurations);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Refreshes the managed configuration with the given configuration name.
     * If you specify {@link #REFRESH_ALL} all managed configuration will be refreshed.
     *
     * @param configurationName the configuration to refresh.
     */
    public void refresh(String configurationName) {
        Assert.notNull(configurationName);
        final boolean refreshAll = REFRESH_ALL.equals(configurationName);
        getManagedConfigurations().forEach(new Consumer<ManagedConfiguration>() {
            @Override
            public void accept(ManagedConfiguration configuration) {
                if (refreshAll || configuration.getConfigurationName().equals(configurationName)) {
                    try {
                        configuration.configure(configurationProvider, sessionTemplate, true);
                    } catch (ManagedConfigurationException e) {
                        logger.error("Error while refreshing " + configuration.getNodePath() + ", skipping refresh for this managed node.", e);
                    } catch (IllegalStateException e) {
                        //should not happpen, unless node has been deleted in the mean time
                        untrack(configuration, e);
                    }
                }
            }
        });
    }

    /**
     * Convenience method for {@link #refresh(String)} with parameter {@link #REFRESH_ALL}.
     *
     * @see #refresh(String)
     */
    @SuppressWarnings("unused")
    public void refreshAll() {
        refresh(REFRESH_ALL);
    }

    /**
     * Convenience method for {@link #refresh(String)} with parameter {@link #REFRESH_ALL}.
     *
     * @see #refresh(String)
     */
    @SuppressWarnings("unused")
    public void refresh(Node node) {
        try {
            refresh(node.getName());
        } catch (RepositoryException e) {
            logger.error("Error refreshing managed configuration for node, skipping refresh...");
        }
    }

    @Activate
    public void activate(ComponentContext componentContext) {
        try {
            this.sessionTemplate = new JcrSessionTemplate<>(repository);
        } catch (Exception e) {
            logger.error("FATAL - Error activating " + this.getClass().getName() + " component.", e);
        }
    }

    @Deactivate
    public void deactivate(ComponentContext componentContext) {
        try {
            sessionTemplate = null;
            lock.writeLock().lock();
            managedConfigurations = new HashSet<>();
        } catch (Exception e) {
            //ignore, nothing we can do anymore...
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unused")
    public void bindConfigurationProvider(ConfigurationProvider configurationProvider) {
        this.configurationProvider = configurationProvider;
    }

    @SuppressWarnings("unused")
    public void unbindConfigurationProvider(ConfigurationProvider configurationProvider) {
        this.configurationProvider = null;
    }

    @SuppressWarnings("unused")
    public void bindSlingRepository(SlingRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("unused")
    public void unbindSlingRepository(SlingRepository repository) {
        this.repository = null;
    }

    @SuppressWarnings("unused")
    public void bindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }

    @SuppressWarnings("unused")
    public void unbindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = null;
    }

    //    private RefreshRemoteApplicationEvent read(byte[] payload) throws Exception {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new SubtypeModule());
//        ObjectReader reader = mapper.readerFor(RefreshRemoteApplicationEvent.class);
//        return reader.readValue(payload);
//    }

//                            try {
//                                RefreshRemoteApplicationEvent event = read(body);
//                            } catch (Exception e) {
//                                logger.error("Error reading payload with jackson", e);
//                            }
//                            try {
//                                Kryo kryo = new Kryo();
//                                kryo.register(RefreshRemoteApplicationEvent.class);
//                                Input input = new Input(new ByteArrayInputStream(body));
//                                RefreshRemoteApplicationEvent refreshEvent = kryo.readObject(input, RefreshRemoteApplicationEvent.class);
//                                input.close();
//
//                                // (process the message components here ...)
//                                channel.basicAck(deliveryTag, false);
//                            } catch (Exception e) {
//                                logger.error("Error reading payload with kryo", e);
//                            }
}
