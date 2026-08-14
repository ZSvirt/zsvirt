package org.zstack.zops;


public class CephHealthInfo {
    private String hostname;

    private String model = ZOpsConstants.UNKNOWN;

    private CephHealthStatus status;

    private String detail;
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public CephHealthInfo(String hostname, String detail) {
        this.hostname = hostname;
        this.detail = detail;
    }

    public void setStatus(CephHealthStatus status) {
        this.status = status;
    }

    public CephHealthStatus getStatus() {
        return status;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }
}
