package org.gbif.datarepo.registry;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    // determines whether encountering of unknown properties (ones that do not map to a property, and there is no
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return MAPPER;
  }

}
