package com.bose.services.acms.client.hybris;

import com.bose.services.acms.api.ConfigurationClientException;
import com.bose.services.acms.api.ConfigurationRefreshListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Configures the Application Configuration Management client and service for Hybris.
 */
public class ApplicationConfigurationServiceConfigurer extends PropertySourcesPlaceholderConfigurer implements InitializingBean, DisposableBean{
    private static final String ACMS_CLIENT_CONFIG = "acms-client-config-hybris";
    private ConfigurationRefreshEventListener refreshEventListener;

    /**
     * The configuration name to use, defaults to the Hybris extension name.
     */
    private String configurationName;
    /**
     * Additional profiles to use, these will be added to any existing active profiles when querying
     * for configuration.
     * Optional.
     */
    private String profiles;

    private String label;

    private PropertySourceLocator propertySourceLocator;

    private ConfigurableEnvironment environment;

    public ApplicationConfigurationServiceConfigurer() {
        this.setIgnoreUnresolvablePlaceholders(true);
        this.setLocalOverride(false);
        this.refreshEventListener = new ConfigurationRefreshEventListener(new RefreshListener());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        refreshEventListener.open();
        locateRemotePropertySources();
    }

    @Override
    public void destroy() {
        if (refreshEventListener != null) {
            refreshEventListener.close();
        }
    }

    private void initializeEnvironment() {
        //not sure we want this, because then we cannot supported multiple of these beans in a context.
        Set<String> existingProfiles = new HashSet<>();
        Collections.addAll(existingProfiles, this.environment.getActiveProfiles());
        if (StringUtils.hasText(getProfiles())) {
            String[] profiles = StringUtils.commaDelimitedListToStringArray(getProfiles());
            for (String profile : profiles) {
                if (!existingProfiles.contains(profile)) {
                    this.environment.addActiveProfile(profile);
                }
            }
        }
    }

    protected Environment getEnvironmentForLocator() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(getProfiles());
        Map<String, Object> config = new HashMap<>();
        config.put(ConfigClientProperties.PREFIX + ".name", getConfigurationName());
        config.put(ConfigClientProperties.PREFIX + ".profile", getProfiles());
        config.put(ConfigClientProperties.PREFIX + ".label", getLabel());
        environment.getPropertySources().addFirst(new MapPropertySource(ACMS_CLIENT_CONFIG, config));
        return environment;
    }

    protected void locateRemotePropertySources() {
        Environment locatorEnv = getEnvironmentForLocator();
        if (this.propertySourceLocator == null) {
            this.propertySourceLocator = new ConfigServicePropertySourceLocator(new ConfigClientProperties(locatorEnv));
        }
        this.environment.getPropertySources().addFirst(propertySourceLocator.locate(locatorEnv));
    }

    @Override
    public void setEnvironment(Environment environment) {
        super.setEnvironment(environment);
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5000;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    class RefreshListener implements ConfigurationRefreshListener {
        @Override
        public boolean refresh(String name) throws ConfigurationClientException {
            if (ConfigurationRefreshListener.REFRESH_ALL.equalsIgnoreCase(name)
                    || configurationName.equalsIgnoreCase(name)) {
                locateRemotePropertySources();
                return true;
            } else {
                logger.info("Ignoring refresh event for configuration name " + name);
                return false;
            }
        }
    }
}
