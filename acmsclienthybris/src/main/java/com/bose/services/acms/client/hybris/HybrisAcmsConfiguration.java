package com.bose.services.acms.client.hybris;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstraps our spring configuration.
 */
@Configuration
@ComponentScan
public class HybrisAcmsConfiguration  {
    @Autowired
    private ConfigurableEnvironment environment;

    //@Value("${some.prop:Some default value instead...}")
    private String testValue;

    public ConfigClientProperties configClientProperties() {
        ConfigClientProperties client = new ConfigClientProperties(this.environment);
        return client;
    }

    @Bean
    public String testBean(@Value("${tax.url:whoops}") String someValue){
        return this.testValue = someValue;
    }

    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(final ConfigurableEnvironment environment){
        //TODO: do something sensible with these client props
        //TODO: provide an easy way to set spring application name prop, so devs don't need to add a application.properties file...
        environment.setActiveProfiles("dev, us");
        Map<String,Object> config = new HashMap<>();
        config.put(ConfigClientProperties.PREFIX + ".name", "tax");
        config.put(ConfigClientProperties.PREFIX + ".profile", "dev,us");
        config.put(ConfigClientProperties.PREFIX + ".label", "");
        environment.getPropertySources().addFirst(new MapPropertySource("acms-client-config", config));
        ConfigClientProperties clientProperties = new ConfigClientProperties(environment);
        ConfigServicePropertySourceLocator locator = new ConfigServicePropertySourceLocator(clientProperties);
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(locator.locate(environment));
        configurer.setPropertySources(propertySources);
        return configurer;
    }

    /*@Bean
    @ConditionalOnProperty(value = "spring.cloud.config.enabled", matchIfMissing = true)
    public ConfigServicePropertySourceLocator configServicePropertySource() {
        ConfigServicePropertySourceLocator locator = new ConfigServicePropertySourceLocator(
                configClientProperties());
        return locator;
    }*/

}
