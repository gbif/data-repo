package org.gbif.datarepo.persistence.mappers;


import org.gbif.datarepo.persistence.model.DBLoggingEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.db.DBAppender;
import ch.qos.logback.core.db.DataSourceConnectionSource;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Tests for class {@link LoggingMapper}.
 */
public class LoggingMapperTest extends BaseMapperTest {

    //Guice injector used to instantiate Mappers.
    private static Injector injector;

    //Logback logger context
    private static LoggerContext loggerContext;

    //It is controlled as static instance because it needs to be started and stopped
    private static DBAppender dbAppender;

    /**
     * Factory method to create a LoggerContext programmatically.
     * @return a new instance of logback logger context
     */
    private static LoggerContext buildLoggerContext() {
      LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
      loggerContext.start();
      return loggerContext;
    }

    /**
     * Factory method that creates a DBAppender that uses the embedded PostgresSQL database.
     * @return a  started DBAppender that uses an embedded PostgreSQL database
     */
    private static DBAppender buildDBAppender() {
        DBAppender dbAppender = new DBAppender();
        DataSourceConnectionSource dataSourceConnectionSource = new DataSourceConnectionSource();
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setMaximumPoolSize(1);
        hikariDataSource.setMinimumIdle(1);
        hikariDataSource.setPoolName("datarepo");
        hikariDataSource.setJdbcUrl(getJdbcUrl());
        hikariDataSource.setAutoCommit(true);
        dataSourceConnectionSource.setDataSource(hikariDataSource);
        dataSourceConnectionSource.discoverConnectionProperties();
        dbAppender.setConnectionSource(dataSourceConnectionSource);
        dbAppender.setContext(loggerContext);
        dbAppender.start();
        return dbAppender;
    }

    /**
     * Factory method to create a Logger instance that uses a DBAppender.
     * @return a new Logger instance that uses a DBAppender
     */
    private static Logger buildLogger() {
        Logger log = loggerContext.getLogger(LoggingMapperTest.class);
        log.addAppender(dbAppender);
        log.setLevel(Level.INFO);
        return log;
    }


    /**
     * Initializes the MyBatis module.
     */
    @BeforeClass
    public static void init() {
      injector = buildInjector();
      loggerContext = buildLoggerContext();
      dbAppender = buildDBAppender();
    }

    @AfterClass
    public static void tearDownAll() {
      //Stops the DBAppender
      if (dbAppender != null) {
        dbAppender.stop();
      }
      //Stops the LoggerContext
      if (loggerContext != null) {
        loggerContext.stop();
      }
      //Stops the embedded DB server
      tearDown();
    }

    /**
     * Asserts that the count and list.size match.
     */
    private static void assertCountsOnList(long count, List<DBLoggingEvent> loggingEvents, long countExpected) {
      Assert.assertEquals("Expected count differs from count", count, countExpected);
      Assert.assertEquals("Count and list contain different results", count, loggingEvents.size());
    }

    /**
     * Multiple test cases to validate that the MyBatis query service retrieves data correctly.
     */
    @Test
    public void testList() {
      LoggingMapper loggingMapper = injector.getInstance(LoggingMapper.class);
      long now = Instant.now().toEpochMilli();
      long tomorrow = Instant.now().plus(1,ChronoUnit.DAYS).toEpochMilli();

      //Creates a Logger that writes into the embedded DB a test message
      Logger dbLogger = buildLogger();

      //Log error
      dbLogger.error("Test error", new IllegalStateException("Inner", new IllegalStateException("Nested")));

      //Log using MDC
      Map<String,String> mdc = Collections.singletonMap("TestMDCKey","TestMDCValue");
      MDC.setContextMap(mdc);
      dbLogger.info("Test logging message");
      MDC.clear();

      //Test a find all query
      assertCountsOnList(loggingMapper.count(null, null, null, null),
                         loggingMapper.list(null, null, null, null),
                         2);


      //Test a find by mdc query
      assertCountsOnList(loggingMapper.count(null, null, mdc, null),
                         loggingMapper.list(null, null, mdc, null),
                         1);


      //Test a find by mdc query
      assertCountsOnList(loggingMapper.count(now, tomorrow, null, null),
                         loggingMapper.list(now, tomorrow, null, null),
                         2);

      //Expects nothing from tomorrow
      assertCountsOnList(loggingMapper.count(tomorrow, null, null, null),
                         loggingMapper.list(tomorrow, null, null, null),
                         0);
    }
}
