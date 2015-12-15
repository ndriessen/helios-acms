package com.bose.services.acms.api;

/**
 * Created by niki on 15/12/15.
 */
public interface ConfigurationRefreshListener {
    String REFRESH_ALL = "*";
    boolean refresh(String name) throws ConfigurationClientException;
}
