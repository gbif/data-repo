package org.gbif.datarepo.persistence.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.google.common.primitives.Ints;

/**
 * Data transfer object to extract information of logs created by the {@link ch.qos.logback.classic.db.DBAppender}.
 */
public class DBLoggingEvent {

    /**
     * Represents a {@link org.slf4j.MDC} entry.
     * It was crated as a convenient way to map results from MyBatis.
     */
    public static class MDCEntry {

      private final String key;
      private final String value;

      public MDCEntry(String key, String value) {
        this.key = key;
        this.value = value;
      }

        public String getKey() {
        return key;
      }

      public String getValue() {
        return value;
      }

      @Override
      public boolean equals(Object o) {
          if(this == o) {
            return true;
          }
          if(o == null || getClass() != o.getClass()) {
              return false;
          }
          MDCEntry mdcEntry = (MDCEntry) o;
          return Objects.equals(key, mdcEntry.key) && Objects.equals(value, mdcEntry.value);
      }

      @Override
      public int hashCode() {
        return Objects.hash(key, value);
      }

        @Override
        public String toString() {
            return "MDCEntry{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
        }
    }

    private Long timestamp;
    private String formattedMessage;
    private String loggerName;
    private String level;
    private String threadName;
    private Integer referenceFlag;
    private String arg0;
    private String arg1;
    private String arg2;
    private String arg3;
    private String callerFilename;
    private String callerClass;
    private String callerMethod;
    private String callerLine;
    private Long eventId;

    private final Map<String,String> mdc = new HashMap<>();

    private List<String> stackTrace;

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public void setFormattedMessage(String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public Integer getReferenceFlag() {
        return referenceFlag;
    }

    public void setReferenceFlag(Integer referenceFlag) {
        this.referenceFlag = referenceFlag;
    }

    public String getArg0() {
        return arg0;
    }

    public void setArg0(String arg0) {
        this.arg0 = arg0;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    public String getArg3() {
        return arg3;
    }

    public void setArg3(String arg3) {
        this.arg3 = arg3;
    }

    public String getCallerFilename() {
        return callerFilename;
    }

    public void setCallerFilename(String callerFilename) {
        this.callerFilename = callerFilename;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public void setCallerClass(String callerClass) {
        this.callerClass = callerClass;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public void setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
    }

    public String getCallerLine() {
        return callerLine;
    }

    public void setCallerLine(String callerLine) {
        this.callerLine = callerLine;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Set<MDCEntry> getMdc() {
        return mdc.entrySet().stream()
            .map(entry -> new MDCEntry(entry.getKey(), entry.getValue()))
            .collect(Collectors.toSet());
    }

    public MDCEntry getMdc(String key) {
      return Optional.ofNullable(mdc.get(key)).map(value -> new MDCEntry(key,value)).orElse(null);
    }

    public void setMdc(Set<MDCEntry> mdc) {
        mdc.clear();
        mdc.forEach(mdcEntry -> this.mdc.put(mdcEntry.getKey(), mdcEntry.getValue()));
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if(o == null || getClass() != o.getClass()) {
        return false;
      }
      DBLoggingEvent that = (DBLoggingEvent) o;
      return Objects.equals(timestamp, that.timestamp)
             && Objects.equals(formattedMessage, that.formattedMessage)
             && Objects.equals(loggerName, that.loggerName)
             && Objects.equals(level, that.level)
             && Objects.equals(threadName, that.threadName)
             && Objects.equals(referenceFlag, that.referenceFlag)
             && Objects.equals(arg0, that.arg0)
             && Objects.equals(arg1, that.arg1)
             && Objects.equals(arg2, that.arg2)
             && Objects.equals(arg3, that.arg3)
             && Objects.equals(callerFilename, that.callerFilename)
             && Objects.equals(callerClass, that.callerClass)
             && Objects.equals(callerMethod, that.callerMethod)
             && Objects.equals(callerLine, that.callerLine)
             && Objects.equals(eventId, that.eventId)
             && Objects.equals(mdc, that.mdc)
             && Objects.equals(stackTrace, that.stackTrace);
    }

    @Override
    public int hashCode() {
      return Objects.hash(timestamp,
                          formattedMessage,
                          loggerName,
                          level,
                          threadName,
                          referenceFlag,
                          arg0,
                          arg1,
                          arg2,
                          arg3,
                          callerFilename,
                          callerClass,
                          callerMethod,
                          callerLine,
                          eventId,
                          mdc,
                          stackTrace);
    }

    @Override
    public String toString() {
      return "DBLoggingEvent{"
             + "timestamp="
             + timestamp
             + ", formattedMessage='"
             + formattedMessage
             + '\''
             + ", loggerName='"
             + loggerName
             + '\''
             + ", level='"
             + level
             + '\''
             + ", threadName='"
             + threadName
             + '\''
             + ", referenceFlag="
             + referenceFlag
             + ", arg0='"
             + arg0
             + '\''
             + ", arg1='"
             + arg1
             + '\''
             + ", arg2='"
             + arg2
             + '\''
             + ", arg3='"
             + arg3
             + '\''
             + ", callerFilename='"
             + callerFilename
             + '\''
             + ", callerClass='"
             + callerClass
             + '\''
             + ", callerMethod='"
             + callerMethod
             + '\''
             + ", callerLine='"
             + callerLine
             + '\''
             + ", eventId="
             + eventId
             + ", mdc="
             + mdc
             + ", stackTrace="
             + stackTrace
             + '}';
    }

    public LoggingEvent toLoggingEvent() {
      LoggingEvent loggingEvent = new LoggingEvent();
      loggingEvent.setLevel(Level.toLevel(level));
      loggingEvent.setLoggerName(loggerName);
      loggingEvent.setThreadName(threadName);
      loggingEvent.setMDCPropertyMap(getMdc().stream().collect(Collectors.toMap(MDCEntry::getKey, MDCEntry::getValue)));
      loggingEvent.setMessage(formattedMessage);
      loggingEvent.setTimeStamp(timestamp);
      loggingEvent.setCallerData(new StackTraceElement[]{new StackTraceElement(callerClass, callerMethod,
                                                                               callerFilename,
                                                                               Ints.tryParse(callerLine))});
      loggingEvent.setArgumentArray(Arrays.asList(arg0, arg1, arg2, arg3).toArray());
      return  loggingEvent;

    }
}
