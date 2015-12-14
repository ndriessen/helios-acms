package com.bose.services.acms.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Client for ACMS.
 *
 */
public class AcmsRestClient {
    private static final Logger logger = LoggerFactory.getLogger(AcmsRestClient.class);
    private static final String DEFAULT_ACMS_URL = "http://localhost:8888";
    private String acmsBaseURL;

    public AcmsRestClient(String acmsBaseURL) {
        this.acmsBaseURL = acmsBaseURL;
    }

    public AcmsRestClient() {
        this(DEFAULT_ACMS_URL);
    }

    public Map<String,String> getConfiguration(String label, String name, String... profiles) {
        Map<String, String> properties = new HashMap<String, String>();
        RestTemplate restTemplate = new RestTemplate();
        String profileList = StringUtils.arrayToCommaDelimitedString(profiles);
        try {
            AcmsResponse response = restTemplate.getForObject(
                    String.format(acmsBaseURL, name, profileList),
                    AcmsResponse.class);
            if (response != null) {
                //TODO: handle ecryption/decryption etc. Investigate aem's crypto service to see if we can store encrypted keys instead of plain text passes etc.
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
                logger.info("Retrieved {} properties for '{}'", properties.size(), name);
                if (logger.isDebugEnabled()) {
                    for (String key : properties.keySet()) {
                        //TODO: boo boo for passwords etc, not even on debug!
                        logger.debug("** {} = {}", key, properties.get(key));
                    }
                }
            }
        } catch (RestClientException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
