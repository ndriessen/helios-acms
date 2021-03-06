package com.bose.services.acms.client.aem.impl;

import com.bose.services.acms.client.aem.ConfigurationProvider;
import com.bose.services.acms.client.aem.ManagedConfigurationException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link ConfigurationProvider} that connects to the ACMS Service.
 *
 * @author Niki Driessen
 */
@Component(name = "AcmsConfigurationProvider")
@Service(ConfigurationProvider.class)
public class AcmsConfigurationProviderImpl implements ConfigurationProvider {
    private static final Logger logger = LoggerFactory.getLogger(AcmsConfigurationProviderImpl.class);
    private static final String CONFIG_SERVER_URL = "http://localhost:8888/%s/%s";
    private static final String DEFAULT_PROFILE = "default"; //only used if no runmodes are active

    @Reference
    private SlingSettingsService slingSettings;
    private Set<String> runModes;
    private Map<String, Properties> configurationCache;

    public AcmsConfigurationProviderImpl() {
    }

    public void bindSlingSettings(SlingSettingsService slingSettingsService) {
        this.slingSettings = slingSettingsService;
    }

    public void unbindSlingSettings(SlingSettingsService slingSettingsService) {
        this.slingSettings = null;
    }

    @Activate
    public void activate() {
        this.configurationCache = new ConcurrentHashMap<>();
        this.runModes = new HashSet<>();
        if (this.slingSettings != null) {
            this.runModes = this.slingSettings.getRunModes();
            logger.info("Setting base configuration profiles to runmode list: {}", StringUtils.collectionToCommaDelimitedString(this.runModes));
        }
    }

    protected List<String> getFinalProfiles(String... additionalProfiles) {
        List<String> result = new ArrayList<>();
        result.addAll(this.runModes);
        //add additional as last so they are always "more specific"
        if (additionalProfiles != null) {
            for (String profile : additionalProfiles) {
                if (!result.contains(profile)) {
                    result.add(profile);
                }
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            result.add(DEFAULT_PROFILE);
        }
        return result;
    }

    @Override
    /**
     * Refreshes the configuration.
     */
    public boolean refresh(String name, String... additionalProfiles) throws ManagedConfigurationException {
        Properties newProperties = getConfiguration(name, additionalProfiles);
        String cacheKey = getCacheKey(name, additionalProfiles);
        Properties currentProperties = this.configurationCache.get(cacheKey);
        if (currentProperties == null || !newProperties.equals(currentProperties)) {
            this.configurationCache.put(cacheKey, newProperties);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Properties getProperties(String name, String... additionalProfiles) throws ManagedConfigurationException {
        String cacheKey = getCacheKey(name, additionalProfiles);
        if (!this.configurationCache.containsKey(cacheKey)) {
            this.configurationCache.put(cacheKey, getConfiguration(name, additionalProfiles));
        }
        return this.configurationCache.get(cacheKey);
    }

    private String getCacheKey(String name, String[] additionalProfiles) {
        return name + "#" + StringUtils.arrayToCommaDelimitedString(additionalProfiles);
    }

    protected Properties getConfiguration(String name, String... additionalProfiles) throws ManagedConfigurationException {
        try {
            Properties dictionary = new Properties();
            RestTemplate restTemplate = new RestTemplate();
            String profileList = StringUtils.collectionToCommaDelimitedString(getFinalProfiles(additionalProfiles));
            logger.info("Querying service for configuration with name '{}' and runModes '{}'", name, profileList);
            RemoteConfig response = restTemplate.getForObject(
                    String.format(CONFIG_SERVER_URL, name, profileList),
                    RemoteConfig.class);
            if (response != null) {
                //TODO: handle ecryption/decryption etc. Investigate aem's crypto service to see if we can store encrypted keys instead of plain text passes etc.
                Map<String, String> properties = new HashMap<>();
                for (PropertySource source : response.getPropertySources()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (Map<String, String>) source
                            .getSource();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        if (!properties.containsKey(entry.getKey())) {
                            properties.putIfAbsent(entry.getKey(), entry.getValue());
                        }
                    }
                }
                dictionary.putAll(properties);
                logger.info("Retrieved {} properties for '{}'", properties.size(), name);
                if (logger.isDebugEnabled()) {
                    for (String key : properties.keySet()) {
                        //TODO: boo boo for passwords etc, not even on debug!
                        logger.debug("** {} = {}", key, properties.get(key));
                    }
                }
            }
            return dictionary;
        } catch (Throwable e) {
            throw new ManagedConfigurationException("Error while quering config service for configuration", e);
        }
    }
}
