package org.zstack.network.service.portforwarding;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.vip.VipInventory;

import java.io.Serializable;

/**
 */
public class PortForwardingStruct implements Serializable {
    private L3NetworkInventory vipL3Network;
    private L3NetworkInventory guestL3Network;
    private VipInventory vip;
    private String guestIp;
    private String guestMac;
    private PortForwardingRuleInventory rule;
    private boolean snatInboundTraffic;
    private boolean releaseVmNicInfoWhenDetaching;
    private boolean releaseVip;
    /* when port PortForwarding is applied to router, it will be applied to peer router also.
    * this flag will indicate that peer router should not apply to original router again */
    private boolean applyToHaRouter=true;

    public boolean isReleaseVip() {
        return releaseVip;
    }

    public void setReleaseVip(boolean releaseVip) {
        this.releaseVip = releaseVip;
    }

    public boolean isReleaseVmNicInfoWhenDetaching() {
        return releaseVmNicInfoWhenDetaching;
    }

    public void setReleaseVmNicInfoWhenDetaching(boolean releaseVmNicInfoWhenDetaching) {
        this.releaseVmNicInfoWhenDetaching = releaseVmNicInfoWhenDetaching;
    }

    public boolean isSnatInboundTraffic() {
        return snatInboundTraffic;
    }

    public void setSnatInboundTraffic(boolean snatInboundTraffic) {
        this.snatInboundTraffic = snatInboundTraffic;
    }

    public L3NetworkInventory getVipL3Network() {
        return vipL3Network;
    }

    public L3NetworkInventory getGuestL3Network() {
        return guestL3Network;
    }

    public void setGuestL3Network(L3NetworkInventory guestL3Network) {
        this.guestL3Network = guestL3Network;
    }

    public void setVipL3Network(L3NetworkInventory vipL3Network) {
        this.vipL3Network = vipL3Network;
    }

    public VipInventory getVip() {
        return vip;
    }

    public void setVip(VipInventory vip) {
        this.vip = vip;
    }

    public PortForwardingRuleInventory getRule() {
        return rule;
    }

    public void setRule(PortForwardingRuleInventory rule) {
        this.rule = rule;
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public String getGuestMac() {
        return guestMac;
    }

    public void setGuestMac(String guestMac) {
        this.guestMac = guestMac;
    }

    public boolean isApplyToHaRouter() {
        return applyToHaRouter;
    }

    public void setApplyToHaRouter(boolean applyToHaRouter) {
        this.applyToHaRouter = applyToHaRouter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("port forwarding rule:");
        sb.append(String.format("\n[uuid:%s, vip:%s]", rule.getUuid(), vip.getIp()));
        sb.append(String.format("\n[vip start port:%s, vip end port:%s, allowed CIDR:%s]", rule.getVipPortStart(), rule.getVipPortEnd(), rule.getAllowedCidr()));
        sb.append(String.format("\n[guest start port:%s, guest end port:%s, guest ip:%s]", rule.getPrivatePortStart(), rule.getPrivatePortEnd(), guestIp));
        sb.append(String.format("\n[vm nic uuid:%s]", rule.getVmNicUuid()));
        return sb.toString();
    }
}
