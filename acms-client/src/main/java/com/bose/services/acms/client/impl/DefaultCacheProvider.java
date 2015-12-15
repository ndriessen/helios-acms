package com.bose.services.acms.client.impl;

import com.bose.services.acms.api.Configuration;
import com.bose.services.acms.api.ConfigurationCacheProvider;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Simple implementation that caches in memory.
 */
public class DefaultCacheProvider implements ConfigurationCacheProvider {
    private static final String SEPARATOR = "#";
    private Map<String, Configuration> cache;

    public DefaultCacheProvider() {
        this.cache = Collections.synchronizedMap(new HashMap<String, Configuration>());
    }

    private String getCacheKey(String label, String name, String... profiles) {
        return label + SEPARATOR + name + SEPARATOR + StringUtils.arrayToCommaDelimitedString(profiles);
    }

    @Override
    public Configuration get(String label, String name, String... profiles) {
        return cache.get(getCacheKey(label, name, profiles));
    }

    @Override
    public void cache(Configuration configuration) {
        if (configuration != null) {
            cache.put(getCacheKey(configuration.getLabel(), configuration.getName(), configuration.getProfilesAsArray()), configuration);
        }
    }

    @Override
    public boolean contains(String partialKey, boolean partialKeyMatching) {
        for (String key : cache.keySet()) {
            if((partialKeyMatching && key.contains(partialKey))
                    || (!partialKeyMatching && key.equalsIgnoreCase(partialKey))){
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Configuration> evict(String name, boolean partialKeyMatching) {
        List<Configuration> evicted = new ArrayList<>();
        Set<Map.Entry<String, Configuration>> it = cache.entrySet();
        for (Map.Entry<String, Configuration> entry : it) {
            if ((partialKeyMatching && entry.getKey().contains(name))
                    || (!partialKeyMatching && entry.getKey().equalsIgnoreCase(name))) {
                it.remove(entry);
                evicted.add(entry.getValue());
            }
        }
        return evicted;
    }

    @Override
    public void clear() {
        this.cache.clear();
    }
}
