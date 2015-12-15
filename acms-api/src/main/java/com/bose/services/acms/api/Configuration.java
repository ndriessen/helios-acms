package com.bose.services.acms.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Simple VO to represent the response coming from the ACMS Service.
 * <p>
 * Any client can use this and Jackson to deserialize the REST responses from the service.
 * Example:
 * <pre>
 * ResponseEntity<Configuration> response = null;
 * try {
 *      response = restTemplate.exchange("/some/endpoint/",
 *              HttpMethod.GET, new HttpEntity<Void>((Void) null),
 *              Configuration.class, args);
 * } catch (HttpClientErrorException e) {
 *      if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
 *          throw e;
 *      }
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {
    private String name;
    private String label;
    private String version;
    private List<String> profiles;
    private List<PropertySource> propertySources;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public String[] getProfilesAsArray() {
        if(profiles == null || profiles.size() == 0){
            return new String[]{};
        }
        return profiles.toArray(new String[profiles.size()]);
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    public List<PropertySource> getPropertySources() {
        return propertySources;
    }

    public void setPropertySources(List<PropertySource> propertySources) {
        this.propertySources = propertySources;
    }
}

