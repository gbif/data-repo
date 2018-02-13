package org.gbif.datarepo.resource.logging;

import java.security.Principal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Utility class that logs data packages relevant events.
 */
public class EventLogger {

    //Type field used later by Logstash to route entries into different outputs
    private static final String LOG_TYPE = "datarepo";

    /**
     *  Private constructor.
     */
    private EventLogger() {
      //NOP
    }

    /**
     * Writes an event into the events log.
     */
    private static void log(Logger logger, Principal principal, String identifier, String event, String message) {
        Optional.ofNullable(principal).ifPresent(userPrincipal -> MDC.put("subject", principal.getName()));
        MDC.put("event", event);
        MDC.put("identifier", identifier);
        MDC.put("type", LOG_TYPE);
        logger.info(message);
        MDC.clear();
    }


    /**
     * Writes an CREATE event into the events log.
     */
    public static void logCreate(Logger logger, Principal principal, String identifier) {
        log(logger, principal, identifier, LoggingEvent.CREATE.name(),"Resource created");
    }

    /**
     * Writes an UPDATE event into the events log.
     */
    public static void logUpdate(Logger logger, Principal principal, String identifier) {
        log(logger, principal, identifier, LoggingEvent.UPDATE.name(),"Resource updated");
    }

    /**
     * Writes an DELETE event into the events log.
     */
    public static void logDelete(Logger logger, Principal principal, String identifier) {
        log(logger, principal, identifier, LoggingEvent.DELETE.name(),"Resource deleted");
    }

    /**
     * Writes an READ event into the events log.
     */
    public static void logRead(Logger logger, Principal principal, String identifier) {
        log(logger, principal, identifier, LoggingEvent.READ.name(),"Resource read");
    }

    /**
     * Writes an READ event into the events log.
     */
    public static void logRead(Logger logger, String identifier) {
        log(logger, null, identifier, LoggingEvent.READ.name(),"Resource read");
    }

    /**
     * Writes an LIST event into the events log.
     */
    public static void logList(Logger logger, Principal principal, String identifier) {
        log(logger, principal, identifier, LoggingEvent.LIST.name(),"Listing resources");
    }

}
