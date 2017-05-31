package org.gbif.datarepo.registry;

import org.gbif.api.model.common.DOI;

import java.io.IOException;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Jackson provider, ignores null values, dates are written as timestamps and fails on unknown properties.
 */
@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    SimpleModule module = new SimpleModule();
    module.addSerializer(DOI.class, new DOISerializer());
    MAPPER.registerModule(module);
    // Enforce use from ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  public static class DOISerializer extends StdSerializer<DOI> {

    public DOISerializer() {
      this(null);
    }

    public DOISerializer(Class<DOI> t) {
      super(t);
    }

    @Override
    public void serialize(DOI value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeString(value.toString());
    }
  }

  /**
   * Returns the ObjectMapper instance.
   */
  @Override
  public ObjectMapper getContext(Class<?> type) {
    return MAPPER;
  }

}
