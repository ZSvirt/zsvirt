package org.zstack.sdk;

import org.zstack.sdk.VdiPortInfo;

public class GetVmConsoleAddressResult {
    public java.lang.String hostIp;
    public void setHostIp(java.lang.String hostIp) {
        this.hostIp = hostIp;
    }
    public java.lang.String getHostIp() {
        return this.hostIp;
    }

    public int port;
    public void setPort(int port) {
        this.port = port;
    }
    public int getPort() {
        return this.port;
    }

    public java.lang.String path;
    public void setPath(java.lang.String path) {
        this.path = path;
    }
    public java.lang.String getPath() {
        return this.path;
    }

    public java.lang.String protocol;
    public void setProtocol(java.lang.String protocol) {
        this.protocol = protocol;
    }
    public java.lang.String getProtocol() {
        return this.protocol;
    }

    public VdiPortInfo vdiPortInfo;
    public void setVdiPortInfo(VdiPortInfo vdiPortInfo) {
        this.vdiPortInfo = vdiPortInfo;
    }
    public VdiPortInfo getVdiPortInfo() {
        return this.vdiPortInfo;
    }

}
