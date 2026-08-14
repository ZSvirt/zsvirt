package org.zstack.network.service.eip;

import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.io.Serializable;

/**
 */
public class EipStruct implements Serializable {
    private EipInventory eip;
    private VmNicInventory nic;
    private VipInventory vip;
    private boolean snatInboundTraffic;
    private UsedIpInventory guestIp;
    private String hostUuid;
    private boolean skipArpCheck = false;

    public boolean isSnatInboundTraffic() {
        return snatInboundTraffic;
    }

    public void setSnatInboundTraffic(boolean snatInboundTraffic) {
        this.snatInboundTraffic = snatInboundTraffic;
    }

    public EipInventory getEip() {
        return eip;
    }

    public void setEip(EipInventory eip) {
        this.eip = eip;
    }

    public VmNicInventory getNic() {
        return nic;
    }

    public void setNic(VmNicInventory nic) {
        this.nic = nic;
    }

    public VipInventory getVip() {
        return vip;
    }

    public void setVip(VipInventory vip) {
        this.vip = vip;
    }

    public UsedIpInventory getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(UsedIpInventory guestIp) {
        this.guestIp = guestIp;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isSkipArpCheck() {
        return skipArpCheck;
    }

    public void setSkipArpCheck(boolean skipArpCheck) {
        this.skipArpCheck = skipArpCheck;
    }
}
