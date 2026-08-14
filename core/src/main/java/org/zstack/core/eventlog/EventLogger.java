package org.zstack.core.eventlog;

public interface EventLogger {
    void accept(EventLogBuilder builder);
}
