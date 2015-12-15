package com.bose.services.acms.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Simple VO representing a single configuration source in the response from the ACMS service.
 *
 * @see Configuration
 * @author Niki Driessen
 */
public class PropertySource {

    private String name;

    private Map<?, ?> source;

    public PropertySource() {
    }

    @JsonCreator
    public PropertySource(@JsonProperty("name") String name,
                          @JsonProperty("source") Map<?, ?> source) {
        this.name = name;
        this.source = source;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSource(Map<?, ?> source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public Map<?, ?> getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "PropertySource [name=" + name + "]";
    }

}