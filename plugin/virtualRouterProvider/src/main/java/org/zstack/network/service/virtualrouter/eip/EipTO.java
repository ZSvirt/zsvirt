package org.zstack.network.service.virtualrouter.eip;

import java.io.Serializable;

/**
 */
public class EipTO implements Serializable {
    private String vipIp;
    private String privateMac;
    private String publicMac;
    private String guestIp;
    private boolean snatInboundTraffic;
    private boolean needCleanGuestIp;

    public boolean isNeedCleanGuestIp() {
        return needCleanGuestIp;
    }

    public void setNeedCleanGuestIp(boolean needCleanGuestIp) {
        this.needCleanGuestIp = needCleanGuestIp;
    }

    public boolean isSnatInboundTraffic() {
        return snatInboundTraffic;
    }

    public void setSnatInboundTraffic(boolean snatInboundTraffic) {
        this.snatInboundTraffic = snatInboundTraffic;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public String getVipIp() {
        return vipIp;
    }

    public void setVipIp(String vipIp) {
        this.vipIp = vipIp;
    }

    public String getPrivateMac() {
        return privateMac;
    }

    public void setPrivateMac(String privateMac) {
        this.privateMac = privateMac;
    }

    public String getPublicMac() {
        return publicMac;
    }

    public void setPublicMac(String publicMac) {
        this.publicMac = publicMac;
    }
}
