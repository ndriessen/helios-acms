package com.bose.services.acms.client.impl;

import com.bose.services.acms.api.Configuration;
import com.bose.services.acms.api.ConfigurationClientException;
import com.bose.services.acms.api.ConfigurationHierarchyStrategy;
import com.bose.services.acms.api.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Merges all sources together, respecting profile order. The last profile (most specific) will always win.
 *
 * Example:<br/>
 * If you would have have following props:<br/>
 * <ul>
 *  <li>'default' profile
 *  <ul>
 *      <li>prop1=default value</li>
 *      <li>prop2=default value2</li>
 *  </ul>
 *  </li>
 *  <li>'foo' profile
 *  <ul>
 *      <li>prop1=foo value</li>
 *  </ul>
 *  </li>
 *  <li>'bar' profile
 *  <ul>
 *      <li>prop2=bar value2</li>
 *  </ul>
 *  </li>
 *  <li>'foobar' profile
 *  <ul>
 *      <li>prop2=foobar!</li>
 *  </ul>
 *  </li>
 * </ul>
 * Then this strategy would "merge" as following:
 * <ul>
 *     <li>profiles: none (or 'default'): default value, default value 2</li>
 *     <li>profiles: foo: foo value, default value2</li>
 *     <li>profiles: bar: default value, bar value2</li>
 *     <li>profiles: foo,bar: foo value, bar value2</li>
 *     <li>profiles: foo,foobar: foo value, foobar!</li>
 *     <li>profiles: bar,foobar: foo value, foobar!</li>
 *     <li>profiles: foobar,bar: foo value,bar value2</li>
 * </ul>
 *
 * This is also the default behavior of Acms service (spring cloud config) in that it adds the property source for
 * each profile in the same order, i.e. the last profile specified on the list is considered the most specific one.
 * <br/>
 * This also implies that if you don't use the {@see DefaultConfigurationClient} and use a different client that
 * does something different with the results returned from the server, you also must implement a new strategy to match it.
 *  *
 * @author Niki Driessen.
 */
public class DefaultHierarchyStrategy implements ConfigurationHierarchyStrategy {
    @Override
    public Properties resolveHierarchy(Configuration source) throws ConfigurationClientException {
        Properties dictionary = new Properties();
        if(source != null) {
            Map<String, String> properties = new HashMap<>();
            for (PropertySource propertySource : source.getPropertySources()) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) propertySource
                        .getSource();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (!properties.containsKey(entry.getKey())) {
                        properties.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
            }
            dictionary.putAll(properties);
        }
        return dictionary;
    }
}
