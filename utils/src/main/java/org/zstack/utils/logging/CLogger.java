package org.zstack.utils.logging;


/**
 * Goals:
 * 1. log4j interface
 * 2. two category logs: (1) ordinary log outputting to primary log file (2) supporting log that should be present to 
 *    customer or support team saves to another file
 * 3. use gson to attach parameters to log
 * 4. cooperate with EventKit system, supporting log should be able to be published as event
 * @author frank
 *
 */
public interface CLogger {
    void trace(String msg, Throwable e);

    void trace(String msg);

    void debug(String msg, Throwable e);

    void debug(String msg);

    void info(String msg, Throwable e);

    void info(String msg);

    void warn(String msg, Throwable e);

    void warn(String msg);

    void error(String msg, Throwable e);

    void error(String msg);

    void fatal(String msg, Throwable e);

    void fatal(String msg);
    
    boolean isTraceEnabled();
}
