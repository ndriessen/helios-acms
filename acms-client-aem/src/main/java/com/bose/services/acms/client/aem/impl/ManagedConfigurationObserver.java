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
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    private DelayQueue<DelayedJcrEvent> eventQueue;
    private ExecutorService eventHandlerExecutor;

    abstract class  DelayedJcrEvent implements Delayed {
        private static final int PROCESSING_DELAY_MILLISECS = 5 * 1000; //in milliseconds
        private String nodePath;
        private long startTime;

        public DelayedJcrEvent(String nodePath) {
            this.nodePath = nodePath;
            this.startTime = System.currentTimeMillis() + PROCESSING_DELAY_MILLISECS;
        }

        public String getNodePath() {
            return nodePath;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = startTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (this.startTime < ((DelayedJcrEvent) o).startTime) {
                return -1;
            }
            if (this.startTime > ((DelayedJcrEvent) o).startTime) {
                return 1;
            }
            return 0;
        }

        public abstract void process(Node node, ManagedConfigurationTracker tracker);
    }

    class AddNodeEventHandler extends DelayedJcrEvent {
        public AddNodeEventHandler(String nodePath) {
            super(nodePath);
        }

        @Override
        public void process(Node node, ManagedConfigurationTracker tracker) {
            try {
                tracker.track(node);
            } catch (RepositoryException e) {
                logger.error("Error adding node '" + getNodePath() + "' to track list", e);
            }
        }
    }

    class RemoveNodeEventHandler extends DelayedJcrEvent {
        public RemoveNodeEventHandler(String nodePath) {
            super(nodePath);
        }

        @Override
        public void process(Node node, ManagedConfigurationTracker tracker) {
            try {
                tracker.untrack(getNodePath());
            } catch (RepositoryException e) {
                logger.error("Error removing node '" + getNodePath() + "' from track list", e);
            }
        }
    }

    class EventHandlerRunnable implements Runnable {
        private JcrSessionTemplate<Node> sessionTemplate;
        private ManagedConfigurationTracker tracker;

        public EventHandlerRunnable(ManagedConfigurationTracker tracker, JcrSessionTemplate<Node> sessionTemplate) {
            this.tracker = tracker;
            this.sessionTemplate = sessionTemplate;
        }

        protected JcrSessionTemplate<Node> getSessionTemplate() {
            return sessionTemplate;
        }

        protected ManagedConfigurationTracker getTracker() {
            return tracker;
        }

        @Override
        public void run() {
            try {
                final DelayedJcrEvent event = eventQueue.poll();
                Node node = sessionTemplate.executeWithResult(new JcrSessionTemplate.Callback<Node>() {
                    @Override
                    public Node execute(Session session) throws Exception {
                        try {
                            return session.getNode(event.getNodePath());
                        } catch (RepositoryException e) {
                            return null; //for delete events, this can happen, we can just pass null in to the handler...
                        }
                    }
                });
                event.process(node, tracker);
            } catch (Exception e) {
                logger.error("Error processing (delayed) JCR event", e);
            }
        }
    }

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
                                    //TODO: for some reason, sometimes the event triggers too early, and the "hasMixin" returns false
                                    //TODO: event triggering seems unreliable with race conditions (ie other listeners doing things with our nodes in parallel)
                                    // we might want to schedule processing events on an internal queue with a short delay?
                                    Node node = session.getNode(event.getPath());
                                    tracker.track(node);
                                } else if (event.getType() == Event.NODE_REMOVED) {
                                    tracker.untrack(event.getPath());
                                }
                                //TODO: we should also support property changes within managed config nodes to cover all cases
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
