package com.bose.services.acms.api;

/**
 * API for providers of a refresh channel for configuration changes.
 * A refresh channel opens some underlying communication channel to receive asynchronous 'refresh' events from the
 * configuration service and will trigger any registered {@link ConfigurationRefreshListener}s.
 *
 * @author Niki Driessen
 */
public interface RefreshChannelProvider {
    /**
     * Implementations have to open the channel in this method.
     * If it needs to listen to something, it needs to start it's own thread, the underlying event system should be used
     * to trigger the {@link #onRefresh(String)} method.
     */
    void open();

    /**
     * Implementations should trigger any registered {@link ConfigurationRefreshListener}s and provide them with the updated
     * configuration.
     *
     * @param configurationName the name of the configuration that has changed.
     */
    void onRefresh(String configurationName);

    /**
     * Implementations have to close the channel properly, and without throwing exceptions.
     * Every effort should be made to cleanup all used resources in a proper way.
     */
    void close();
}
