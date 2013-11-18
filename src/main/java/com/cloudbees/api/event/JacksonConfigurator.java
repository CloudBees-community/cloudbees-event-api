package com.cloudbees.api.event;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.ws.rs.ext.ContextResolver;

/**
 * @author Vivek Pandey
 */
public class JacksonConfigurator implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonConfigurator(ObjectMapper mapper) {
        this.mapper = mapper;
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }

    public ObjectMapper getContext(Class<?> arg0) {
        return mapper;
    }
}

