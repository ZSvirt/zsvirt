package org.zstack.log4j2;

import org.apache.logging.log4j.core.config.Configuration;

public interface Log4j2AppenderProxyFactory {
    String createLog4j2Appender(Configuration config, Log4j2ConfigurationStruct struct, String uuid);

    Log4j2AppenderType getLog4j2AppenderType();

    void validate(Log4j2ConfigurationStruct struct);
}
