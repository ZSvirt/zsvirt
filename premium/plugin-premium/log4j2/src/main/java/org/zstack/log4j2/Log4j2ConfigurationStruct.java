package org.zstack.log4j2;

public class Log4j2ConfigurationStruct {
    private String appenderType;
    private String configuration;

    public String getAppenderType() {
        return appenderType;
    }

    public void setAppenderType(String appenderType) {
        this.appenderType = appenderType;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
