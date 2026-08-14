package org.zstack.zops;

public class NetworkReachablePair {
    private String sourceHostname;
    private String targetHostname;

    private HostConnectedStatus status;

    public String getSourceHostname() {
        return sourceHostname;
    }

    public void setSourceHostname(String sourceHostname) {
        this.sourceHostname = sourceHostname;
    }

    public String getTargetHostname() {
        return targetHostname;
    }

    public void setTargetHostname(String targetHostname) {
        this.targetHostname = targetHostname;
    }

    public HostConnectedStatus getStatus() {
        return status;
    }

    public void setStatus(HostConnectedStatus status) {
        this.status = status;
    }
}
