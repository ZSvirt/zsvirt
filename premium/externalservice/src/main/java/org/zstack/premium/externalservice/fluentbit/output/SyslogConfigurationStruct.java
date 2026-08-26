package org.zstack.premium.externalservice.fluentbit.output;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class SyslogConfigurationStruct extends ConfigurationStruct {
    String mode;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
