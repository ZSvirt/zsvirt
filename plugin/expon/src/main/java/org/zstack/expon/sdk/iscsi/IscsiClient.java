package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.Param;

public class IscsiClient {
    @Param(validValues = {"ip"})
    private String hostType;
    @Param
    private String host;

    public String getHostType() {
        return hostType;
    }

    public void setHostType(String hostType) {
        this.hostType = hostType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
