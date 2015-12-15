package com.bose.services.acms.api;

import java.util.Properties;

/**
 * Created by niki on 15/12/15.
 */
public interface ConfigurationRefreshListener {
    String REFRESH_ALL = "*";
    boolean refresh(String name, Properties properties) throws ConfigurationClientException;
}
