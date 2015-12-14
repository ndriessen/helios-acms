package com.bose.services.acms.client.aem;

import java.util.Properties;

/**
 * A configuration provider is used to fetch configuration based on a configuration name and a list of profiles.
 * <p>
 * Providers can implement caching and logic for the profiles as they see fit.
 *
 * @author Niki Driessen
 */
public interface ConfigurationProvider {
    /**
     * Provide properties for the given configuration name and profiles.
     * <p>
     * Implementations must ensure to never return null.
     * implementation can throw a {@link ManagedConfigurationException} to indicate failure, but this should only be done
     * in really fatal situation. Implementations need to implement recovery as much as possible.
     *
     * @param name     the name of the configuration to get, not null.
     * @param profiles an optional list of profiles for the configuration.
     * @return the properties, not null.
     * @throws ManagedConfigurationException can throw this (runtime) exception to indicate critical failure.
     */
    Properties getProperties(String name, String... profiles) throws ManagedConfigurationException;

    /**
     * Check if the configuration has changed since the last call to {@link #getProperties(String, String...)}.
     * <p>
     * If this method returns <code>true</code>, the next call to {@link #getProperties(String, String...)} should return
     * the new configuration. This returns <code>true</code> when the configuration either changed OR if this is the first
     * time the configuration will be loaded.
     *
     * @param name     the name of the configuration to get, not null.
     * @param profiles an optional list of profiles for the configuration.
     * @return <code>true</code> if the configuration has changed since the last call to {@link #getProperties(String, String...)}.
     * @throws ManagedConfigurationException can throw this (runtime) exception to indicate critical failure.
     */
    boolean refresh(String name, String... profiles) throws ManagedConfigurationException;
}
