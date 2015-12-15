package com.bose.services.acms.api;

import java.util.Properties;

/**
 * Strategy interface for handling configuration hierarchy.
 * <p>
 * Implementation receive an ordered list of configuration sources from the acms service and needs to resolve
 * this into a merged map of properties. How merging happens determines hierarchy of the sources and how overriding etc of properties gets done.
 */
public interface ConfigurationHierarchyStrategy {
    Properties resolveHierarchy(Configuration source) throws ConfigurationClientException;
}
