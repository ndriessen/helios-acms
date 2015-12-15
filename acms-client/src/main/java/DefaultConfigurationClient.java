import com.bose.services.acms.api.*;
import com.bose.services.acms.client.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * Basic implementation of an ACMS client.
 *
 * @author Niki Driessen
 */
public class DefaultConfigurationClient implements ConfigurationClient, ConfigurationRefreshListener {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigurationClient.class);

    public static final String DEFAULT_PROFILE = "default";

    private List<ConfigurationRefreshListener> listeners = new ArrayList<>();
    private ClientCryptoProvider cryptoProvider;
    private ConfigurationCacheProvider cacheProvider;
    private ConfigurationHierarchyStrategy configurationHierarchyStrategy;

    private ClientConfiguration configuration;
    private DefaultRestClient client;

    private Set<String> defaultProfiles;

    public DefaultConfigurationClient(String... defaultProfiles) {
        try {
            this.defaultProfiles = new LinkedHashSet<>();
            if (defaultProfiles != null) {
                Collections.addAll(this.defaultProfiles, defaultProfiles);
            }
            this.configuration = new ClientConfiguration();
            this.client = new DefaultRestClient();
            this.cacheProvider = new DefaultCacheProvider();
            this.configurationHierarchyStrategy = new DefaultHierarchyStrategy();
        } catch (IOException e) {
            throw new ConfigurationClientException("Error initializing ACMS configuration client", e);
        }
    }

    public void addRefreshListener(ConfigurationRefreshListener listener){
        this.listeners.add(listener);
    }

    public void removeRefreshListener(ConfigurationRefreshListener listener){
        this.listeners.remove(listener);
    }

    protected void triggerListeners(String name){
        for (ConfigurationRefreshListener listener : listeners) {
            try {
                listener.refresh(name);
            } catch (Exception e) {
                logger.error("Error executing refresh listener", e);
            }
        }
    }

    protected String getFinalProfiles(String... profiles) {
        Set<String> finalList = new LinkedHashSet<>(this.defaultProfiles);
        if (profiles != null) {
            Collections.addAll(finalList, profiles);
        }
        return finalList.isEmpty() ? DEFAULT_PROFILE : StringUtils.collectionToCommaDelimitedString(finalList);
    }

    @Override
    public Properties getConfiguration(String label, String name, String... profiles) throws ConfigurationClientException {
        String profileList = getFinalProfiles(profiles);
        Configuration result = cacheProvider.get(label, name, profileList);
        if (result == null) {
            result = client.getConfiguration(label, name, profileList);
            if (result == null) {
                //also no remote configuration... tsss
                if (configuration.isFailFast()) {
                    throw new ConfigurationClientException(String.format("Could not find configuration for %s:%s (label:%s)", name, profileList, label));
                }
                return new Properties();
            } else {
                cacheProvider.cache(result);
                return configurationHierarchyStrategy.resolveHierarchy(result);
            }
        } else {
            return configurationHierarchyStrategy.resolveHierarchy(result);
        }
    }

    @Override
    public boolean refresh(String name) throws ConfigurationClientException {
        boolean refreshed = false;
        if (RefreshChannelManager.REFRESH_ALL.equalsIgnoreCase(name)) {
            cacheProvider.clear();
            return true;
        } else {
            if (cacheProvider.contains(name, true)) {
                List<Configuration> list = cacheProvider.evict(name, true);
                if (list != null) {
                    for (Configuration conf : list) {
                        getConfiguration(conf.getLabel(), conf.getName(), conf.getProfilesAsArray());
                        refreshed = true;
                    }
                }
            }
        }
        if(refreshed) {
            triggerListeners(name);
        }
        return refreshed;
    }

    @Override
    public Properties getConfiguration(String name, String... profiles) throws ConfigurationClientException {
        return getConfiguration(null, name, profiles);
    }

    @Override
    public void setCryptoProvider(ClientCryptoProvider cryptoProvider) {
        if (cryptoProvider == null) {
            throw new IllegalArgumentException("ClientCryptoProvider can not be <null>");
        }
        this.cryptoProvider = cryptoProvider;
    }

    @Override
    public void setCacheProvider(ConfigurationCacheProvider cacheProvider) {
        if (cacheProvider == null) {
            throw new IllegalArgumentException("ConfigurationCacheProvider can not be <null>");
        }
        this.cacheProvider = cacheProvider;
    }

    @Override
    public void setHierarchyStrategy(ConfigurationHierarchyStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("ConfigurationHierarchyStrategy can not be <null>");
        }
        this.configurationHierarchyStrategy = strategy;
    }

}
