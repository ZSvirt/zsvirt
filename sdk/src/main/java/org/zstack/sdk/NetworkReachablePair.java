package org.zstack.sdk;

import org.zstack.sdk.HostConnectedStatus;

public class NetworkReachablePair  {

    public java.lang.String sourceHostname;
    public void setSourceHostname(java.lang.String sourceHostname) {
        this.sourceHostname = sourceHostname;
    }
    public java.lang.String getSourceHostname() {
        return this.sourceHostname;
    }

    public java.lang.String targetHostname;
    public void setTargetHostname(java.lang.String targetHostname) {
        this.targetHostname = targetHostname;
    }
    public java.lang.String getTargetHostname() {
        return this.targetHostname;
    }

    public HostConnectedStatus status;
    public void setStatus(HostConnectedStatus status) {
        this.status = status;
    }
    public HostConnectedStatus getStatus() {
        return this.status;
    }

}
