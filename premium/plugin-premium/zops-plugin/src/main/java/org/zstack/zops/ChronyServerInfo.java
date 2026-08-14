package org.zstack.zops;

public class ChronyServerInfo {
    private String hostname;
    private HostConnectedStatus status;

    public ChronyServerInfo(String hostname, HostConnectedStatus status) {
        this.hostname = hostname;
        this.status = status;
    }

    public ChronyServerInfo() {
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public HostConnectedStatus getStatus() {
        return status;
    }

    public void setStatus(HostConnectedStatus status) {
        this.status = status;
    }
}
