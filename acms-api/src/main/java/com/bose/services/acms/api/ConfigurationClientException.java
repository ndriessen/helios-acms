package com.bose.services.acms.api;

/**
 * Exception thrown by ACMS clients.
 *
 */
public class ConfigurationClientException extends RuntimeException {
    public ConfigurationClientException(String s) {
        super(s);
    }

    public ConfigurationClientException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
