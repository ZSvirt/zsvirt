package org.zstack.sdk;

import org.zstack.sdk.HostConnectedStatus;

public class ChronyServerInfo  {

    public java.lang.String hostname;
    public void setHostname(java.lang.String hostname) {
        this.hostname = hostname;
    }
    public java.lang.String getHostname() {
        return this.hostname;
    }

    public HostConnectedStatus status;
    public void setStatus(HostConnectedStatus status) {
        this.status = status;
    }
    public HostConnectedStatus getStatus() {
        return this.status;
    }

}
