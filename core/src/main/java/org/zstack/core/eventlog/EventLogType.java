package org.zstack.core.eventlog;

public enum EventLogType {
    Info("info"),
    Warning("warning"),
    Error("error");

    public String name;
    EventLogType(String name) {
        this.name = name;
    }
}
