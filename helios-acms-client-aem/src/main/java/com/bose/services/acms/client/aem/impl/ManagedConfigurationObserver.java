package com.bose.services.acms.client.aem.impl;

import com.bose.services.acms.client.aem.ManagedConfigurationServiceFactory;
import com.bose.services.acms.client.aem.ManagedConfigurationTracker;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

/**
 * JCR Observer that will detect added, removed or changed managed configuration nodes and will inform the
 * {@link ManagedConfigurationTracker} accordingly.
 * <p>
 * These are being registered by the {@link ManagedConfigurationServiceFactory}.
 */
public class ManagedConfigurationObserver implements EventListener {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationObserver.class);
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";

    private String searchPath;
    private JcrSessionTemplate<Void> sessionTemplate;
    private ManagedConfigurationTracker tracker;

    public ManagedConfigurationObserver(String searchPath, ManagedConfigurationTracker tracker, SlingRepository repository) {
        this.tracker = tracker;
        this.searchPath = searchPath;
        this.sessionTemplate = new JcrSessionTemplate<>(repository);
    }

    /**
     * Registers itself as an event listener for the {@link #searchPath}.
     *
     * @param observationManager the JCR observation manager, not null.
     * @throws RepositoryException when registration fails.
     */
    public void register(ObservationManager observationManager) throws RepositoryException {
        try {
            observationManager.addEventListener(
                    this, Event.NODE_ADDED | Event.NODE_REMOVED, searchPath, true, null, null, true);
        } catch (RepositoryException e) {
            logger.error("Error registering JCR Event listener for path " + searchPath, e);
            throw e;
        }
    }

    /**
     * Unregisters itself as a listener.
     *
     * @param observationManager the JCR observation manager, not null.
     */
    public void unregister(ObservationManager observationManager) {
        try {
            observationManager.removeEventListener(this);
        } catch (RepositoryException e) {
            logger.warn("Error removing JCR event listener for path " + searchPath, e);
        }
    }

    /**
     * Process the provided JCR events.
     * <p>
     * If a node gets added that has the {@link ManagedConfigurationMixin#NODE_TYPE} mixin, the node will be
     * tracked by {@link ManagedConfigurationTracker#track(Node)}.
     * If a node gets removed, it will be untracked by {@link ManagedConfigurationTracker#untrack(String)}.
     *
     * @param events the events that this listeners subscribed to.
     */
    public void onEvent(EventIterator events) {
        try {
            if (events != null) {
                //noinspection Convert2Lambda
                sessionTemplate.execute(new JcrSessionTemplate.Callback<Void>() {
                    @Override
                    public Void execute(Session session) throws Exception {
                        while (events.hasNext()) {
                            Event event = (Event) events.next();
                            try {
                                if (event.getType() == Event.NODE_ADDED && hasMixin(event)) {
                                    Node node = session.getNode(event.getPath());
                                    tracker.track(node);
                                } else if (event.getType() == Event.NODE_REMOVED) {
                                    tracker.untrack(event.getPath());
                                }
                                logger.info("Received JCR event for path: " + event.getPath());
                            } catch (Exception e) {
                                logger.error("Error adding/removing JCR node to/from tracked managed configuration", e);
                            }
                        }
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error handling JCR event", e);
        }
    }

    private boolean hasMixin(Event event) throws RepositoryException {
        @SuppressWarnings("unchecked")
        String[] mixins = (String[]) event.getInfo().getOrDefault(JCR_MIXIN_TYPES, new String[0]);
        if (mixins != null && mixins.length > 0) {
            for (String mixin : mixins) {
                if (ManagedConfigurationMixin.NODE_TYPE.equalsIgnoreCase(mixin)) {
                    return true;
                }
            }
        }
        return false;
    }
}
