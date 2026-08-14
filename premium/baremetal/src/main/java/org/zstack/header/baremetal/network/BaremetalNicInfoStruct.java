package org.zstack.header.baremetal.network;

/**
 * Created by GuoYi on 7/5/18.
 */
public class BaremetalNicInfoStruct {
    private String devname;
    private String mac;
    private String speed;
    private Boolean pxe;
    private String ip;

    public String getDevname() {
        return devname;
    }

    public void setDevname(String devname) {
        this.devname = devname;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public Boolean getPxe() {
        return pxe;
    }

    public void setPxe(Boolean pxe) {
        this.pxe = pxe;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
