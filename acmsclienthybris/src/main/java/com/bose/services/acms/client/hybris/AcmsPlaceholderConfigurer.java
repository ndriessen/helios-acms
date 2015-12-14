package com.bose.services.acms.client.hybris;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by niki on 10/12/15.
 */
@Component
public class AcmsPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {
    @Override
    public void setEnvironment(Environment environment) {
        super.setEnvironment(environment);
        Map<String,Object> config = new HashMap<>();
        config.put(ConfigClientProperties.PREFIX + ".name", "tax");
        config.put(ConfigClientProperties.PREFIX + ".profile", "dev,us");
        config.put(ConfigClientProperties.PREFIX + ".label", "");
        ((ConfigurableEnvironment)environment).getPropertySources().addFirst(new MapPropertySource("acms-client-config", config));
        ConfigClientProperties clientProperties = new ConfigClientProperties(environment);
        ConfigServicePropertySourceLocator locator = new ConfigServicePropertySourceLocator(clientProperties);
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(locator.locate(environment));
        setPropertySources(propertySources);
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, ConfigurablePropertyResolver propertyResolver) throws BeansException {
        super.processProperties(beanFactoryToProcess, propertyResolver);
    }
}
