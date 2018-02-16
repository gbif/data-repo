package org.gbif.datarepo.app;

import org.gbif.datarepo.auth.jwt.JwtAuthConfiguration;
import org.gbif.datarepo.impl.conf.DataRepoConfiguration;
import org.gbif.discovery.conf.ServiceConfiguration;

import javax.validation.Valid;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.logging.LoggingFactory;
import io.dropwizard.logging.LoggingUtil;

/**
 * Main service configuration, it contains: DOI settings, required urls and DB settings.
 */
@JsonAutoDetect
public class DataRepoConfigurationDW extends Configuration {

  private static final LogbackAutoConfigLoggingFactory LOGGING_FACTORY = new LogbackAutoConfigLoggingFactory();

  @JsonProperty
  private DataRepoConfiguration dataRepoConfiguration;

  @Valid
  private ServiceConfiguration service;

  @JsonProperty
  private JwtAuthConfiguration jwtAuthConfiguration;

  public DataRepoConfiguration getDataRepoConfiguration() {
    return dataRepoConfiguration;
  }

  public void setDataRepoConfiguration(DataRepoConfiguration dataRepoConfiguration) {
    this.dataRepoConfiguration = dataRepoConfiguration;
  }

  @JsonProperty
  public ServiceConfiguration getService() {
    return service;
  }

  public void setService(ServiceConfiguration service) {
    this.service = service;
  }

  /**
   * JWT authentication configs.
   */
  public JwtAuthConfiguration getJwtAuthConfiguration() {
    return jwtAuthConfiguration;
  }

  public void setJwtAuthConfiguration(JwtAuthConfiguration jwtAuthConfiguration) {
    this.jwtAuthConfiguration = jwtAuthConfiguration;
  }
    @Override
  public LoggingFactory getLoggingFactory() {
        return LOGGING_FACTORY;
    }


  /**
    * https://github.com/dropwizard/dropwizard/issues/1567
    * Override getLoggingFactory for your configuration
  */
  private static class LogbackAutoConfigLoggingFactory implements LoggingFactory {

      @JsonIgnore
      private LoggerContext loggerContext;
      @JsonIgnore
      private final ContextInitializer contextInitializer;

      public LogbackAutoConfigLoggingFactory() {
          loggerContext = LoggingUtil.getLoggerContext();
          contextInitializer = new ContextInitializer(loggerContext);
      }

      @Override
      public void configure(MetricRegistry metricRegistry, String name) {
          try {
              contextInitializer.autoConfig();
          } catch (JoranException e) {
              throw new RuntimeException(e);
          }
      }

      @Override
      public void stop() {
            loggerContext.stop();
        }
  }
}
