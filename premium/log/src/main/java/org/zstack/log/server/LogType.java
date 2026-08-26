package org.zstack.log.server;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum LogType {
    Log4j2,
    FluentBit
}
