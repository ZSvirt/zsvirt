package org.zstack.log.server;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum LogLevel {
    OFF,
    FATAL,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
    ALL
}
