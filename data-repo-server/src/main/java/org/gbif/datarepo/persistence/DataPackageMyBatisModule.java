package org.gbif.datarepo.persistence;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.type.DoiTypeHandler;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.StringArrayTypeHandler;

import java.util.Properties;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.PrivateModule;

/**
 * Persistence MyBatis module. Exposes MyBatis mappers and type required to persist and query DataPackages instances.
 */
public class DataPackageMyBatisModule extends PrivateModule {

  private final InternalDataPackageModule internalModule;

  /**
   * Internal module, it is used to avoid exposing data sources and other MyBatis elements.
   */
  private static class InternalDataPackageModule extends MyBatisModule {

    /**
     * Full constructor.
     * props["poolName"] it's used as thr datasource binding name.
     */
    InternalDataPackageModule(Properties props, MetricRegistry metricRegistry,
                              HealthCheckRegistry healthCheckRegistry) {
      super(props, props.getProperty("poolName"), metricRegistry, healthCheckRegistry);
    }

    /**
     * Bind mappers and aliases.
     */
    @Override
    protected void bindMappers() {
      failFast(true);
      //Aliases
      addAlias("DataPackage").to(DataPackage.class);
      addAlias("DataPackageFile").to(DataPackageFile.class);
      addAlias("DOI").to(DOI.class);
      addAlias("Pageable").to(Pageable.class);
      //Mappers
      addMapperClass(DataPackageMapper.class);
      addMapperClass(DataPackageFileMapper.class);
    }

    /**
     * Bind type handlers.
     */
    @Override
    protected void bindTypeHandlers() {
      addAlias("DoiTypeHandler").to(DoiTypeHandler.class);
    }

    @Override
    protected void bindManagers() {
      //NOP
    }
  }

  /**
   * Full constructor.
   * @param props configuration settings
   * @param metricRegistry dropwizard metrics registry
   * @param healthCheckRegistry dropwizard health check registry
   */
  public DataPackageMyBatisModule(Properties props, MetricRegistry metricRegistry,
                                  HealthCheckRegistry healthCheckRegistry) {
    //Creates the module using provided parameters
    internalModule = new InternalDataPackageModule(props, metricRegistry, healthCheckRegistry);
  }

  @Override
  protected void configure() {
    install(internalModule);
    expose(DataPackageMapper.class);
    expose(DataPackageFileMapper.class);
  }
}
