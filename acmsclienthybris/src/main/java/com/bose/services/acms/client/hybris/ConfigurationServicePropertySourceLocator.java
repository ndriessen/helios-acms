package com.bose.services.acms.client.hybris;

import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.core.env.*;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by niki on 15/12/15.
 */
public class ConfigurationServicePropertySourceLocator {
    private ConfigServicePropertySourceLocator locator = new ConfigServicePropertySourceLocator(new ConfigClientProperties(new StandardEnvironment()));

    public PropertySource<?> locate(String label, String name, String[] profiles) {
        return locator.locate(getEnvironmentForLocator(label, name, profiles));
    }

    protected Environment getEnvironmentForLocator(String label, String name, String... profiles) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profiles);
        Map<String, Object> config = new HashMap<>();
        config.put(ConfigClientProperties.PREFIX + ".name", name);
        config.put(ConfigClientProperties.PREFIX + ".profile", profiles);
        config.put(ConfigClientProperties.PREFIX + ".label", label);
        environment.getPropertySources().addFirst(new MapPropertySource(createPropertySourceName(name, profiles), config));
        return environment;
    }

    private String createPropertySourceName(String name, String... profiles) {
        return String.format("locatorConfig-%s:%s", name, StringUtils.arrayToCommaDelimitedString(profiles));
    }
}
