package org.zstack.premium.externalservice.fluentbit.output;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class ConfigurationStruct {
    String host;
    String port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
