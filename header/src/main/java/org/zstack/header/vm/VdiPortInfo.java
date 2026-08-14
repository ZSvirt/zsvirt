package org.zstack.header.vm;

/**
 * author:kaicai.hu
 * Date:2019/9/16
 */
public class VdiPortInfo {
    private Integer vncPort;
    private Integer spicePort;
    private Integer spiceTlsPort;

    public Integer getVncPort() {
        return vncPort;
    }

    public void setVncPort(Integer vncPort) {
        this.vncPort = vncPort;
    }

    public Integer getSpicePort() {
        return spicePort;
    }

    public void setSpicePort(Integer spicePort) {
        this.spicePort = spicePort;
    }

    public Integer getSpiceTlsPort() {
        return spiceTlsPort;
    }

    public void setSpiceTlsPort(Integer spiceTlsPort) {
        this.spiceTlsPort = spiceTlsPort;
    }

}
