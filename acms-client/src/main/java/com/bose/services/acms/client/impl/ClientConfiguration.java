package com.bose.services.acms.client.impl;

import com.bose.services.acms.api.ConfigurationClientException;

import java.io.IOException;
import java.util.Properties;

/**
 * Holds configuration values.
 */
public class ClientConfiguration {
    public static final String PROP_ACMS_URI = "acms.uri";
    public static final String PROP_ACMS_USERNAME = "acms.username";
    public static final String PROP_ACMS_PASSWORD = "acms.password";
    public static final String PROP_ACMS_DISCOVERY = "acms.discovery.";
    public static final String PROP_ACMS_DISCOVERY_ENABLED = PROP_ACMS_DISCOVERY + "enabled";
    public static final String PROP_ACMS_DISCOVERY_SERVICEID = PROP_ACMS_DISCOVERY + "serviceId";
    public static final String DEFAULT_ACMS_DISCOVERY_SERVICEID = "CONFIGSERVER";
    private static final String DEFAULT_ACMS_URI = "http://localhost:8888";
    private String uri = DEFAULT_ACMS_URI;
    private String username;
    private String password;
    //fail if the config server isn't reachable
    private boolean failFast = false;
    private Discovery discovery;

    public ClientConfiguration() throws ConfigurationClientException {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            throw new ConfigurationClientException("Error reading configuration client properties from classpath:application.properties", e);
        }
        this.uri = properties.getProperty(PROP_ACMS_URI, DEFAULT_ACMS_URI);
        this.username = properties.getProperty(PROP_ACMS_USERNAME);
        this.password = properties.getProperty(PROP_ACMS_PASSWORD);
        this.discovery = new Discovery();
        this.discovery.setEnabled(Boolean.getBoolean(properties.getProperty(PROP_ACMS_DISCOVERY_ENABLED, "false")));
        this.discovery.setServiceId(properties.getProperty(PROP_ACMS_DISCOVERY_SERVICEID, DEFAULT_ACMS_DISCOVERY_SERVICEID));
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    class Discovery {
        private boolean enabled = false;
        private String serviceId = DEFAULT_ACMS_DISCOVERY_SERVICEID;


        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }
    }
}
