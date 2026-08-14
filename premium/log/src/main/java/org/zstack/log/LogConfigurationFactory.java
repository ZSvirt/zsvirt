package org.zstack.log;

import org.zstack.header.core.ReturnValueCompletion;

public interface LogConfigurationFactory {
    void createLogConfiguration(LogConfigurationStruct struct, ReturnValueCompletion<String> completion);

    void deleteLogConfiguration(String uuid);

    void loadConfiguration();

    String getLogConfigurationType();

    void validate(LogConfigurationStruct struct);
}
