package com.bose.services.acms.api;

import java.util.Properties;

/**
 *
 */
public interface ConfigurationClient {
    //Configuration getConfiguration(String label, String name, String... profiles) throws ConfigurationClientException;
    /*String getProperty(String name, String defaultValue);
    String getRequiredProperty(String name, String defaultValue) throws ConfigurationClientException;
    Boolean getProperty(String name, boolean defaultValue);
    Boolean getRequiredProperty(String name, boolean defaultValue) throws ConfigurationClientException;
    Long getProperty(String name, long defaultValue);
    Long getRequiredProperty(String name, long defaultValue) throws ConfigurationClientException;*/

    Properties getConfiguration(String label, String name, String... profiles) throws ConfigurationClientException;
    Properties getConfiguration(String name, String... profiles) throws ConfigurationClientException;
    boolean refresh(String name) throws ConfigurationClientException;

    /**
     * TODO: move to Channel Provider
     * @param listener
     */
    void registerListener(ConfigurationRefreshListener listener);
    /**
     * TODO: move to Channel Provider
     * @param listener
     */
    void unregisterListener(ConfigurationRefreshListener listener);

    void setCryptoProvider(ClientCryptoProvider provider);
    void setCacheProvider(ConfigurationCacheProvider cacheProvider);
    void setHierarchyStrategy(ConfigurationHierarchyStrategy strategy);

}
