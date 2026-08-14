package org.zstack.header.host;

public class VmNicRedirectConfig {
    private Integer mirrorPort;
    private Integer primaryInPort;
    private Integer secondaryInPort;
    private Integer primaryOutPort;
    private Integer deviceId;
    private String driverType;

    public Integer getMirrorPort() {
        return mirrorPort;
    }

    public void setMirrorPort(Integer mirrorPort) {
        this.mirrorPort = mirrorPort;
    }

    public Integer getPrimaryInPort() {
        return primaryInPort;
    }

    public void setPrimaryInPort(Integer primaryInPort) {
        this.primaryInPort = primaryInPort;
    }

    public Integer getSecondaryInPort() {
        return secondaryInPort;
    }

    public void setSecondaryInPort(Integer secondaryInPort) {
        this.secondaryInPort = secondaryInPort;
    }

    public Integer getPrimaryOutPort() {
        return primaryOutPort;
    }

    public void setPrimaryOutPort(Integer primaryOutPort) {
        this.primaryOutPort = primaryOutPort;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }
}
