package com.bose.services.acms.api;

import java.util.List;

/**
 * Interface for a cache provider for caching configurations.
 */
public interface ConfigurationCacheProvider {
    Configuration get(String label, String name, String... profiles);
    void cache(Configuration configuration);
    boolean contains(String partialKey, boolean partialKeyMatching);
    List<Configuration> evict(String key, boolean partialKeyMatching);
    List<Configuration> clear();
}
