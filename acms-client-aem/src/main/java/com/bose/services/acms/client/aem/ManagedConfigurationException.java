package com.bose.services.acms.client.aem;

/**
 * Main exception thrown by the configuration client.
 */
public class ManagedConfigurationException extends RuntimeException {
    public ManagedConfigurationException(String message) {
        super(message);
    }

    public ManagedConfigurationException(String message, Object... params) {
        super(String.format(message, params));
    }

    public ManagedConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ManagedConfigurationException(String message, Throwable throwable, Object... params) {
        super(String.format(message, params), throwable);
    }
}
