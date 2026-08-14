package org.zstack.log;

public interface LogConfigurationManager {
    LogConfigurationFactory getLogConfigurationFactory(String type);
}
