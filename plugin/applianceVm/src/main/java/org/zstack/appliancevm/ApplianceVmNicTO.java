package org.zstack.appliancevm;

import org.zstack.core.db.Q;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.network.IPv6Constants;

/**
 */
public class ApplianceVmNicTO {
    private String ip;
    private String netmask;
    private String gateway;
    private String mac;
    private String deviceName;
    private String metaData;
    private boolean isDefaultRoute;
    private String category;
    private String l2type;
    private Integer vni;
    private String physicalInterface;
    private Integer mtu;
    private String ip6;
    private Integer prefixLength;
    private String gateway6;
    private String addressMode;

    public ApplianceVmNicTO(VmNicInventory inv) {
        for (UsedIpInventory uip : inv.getUsedIps()) {
            if (uip.getIpVersion() == IPv6Constants.IPv4) {
                ip = uip.getIp();
                netmask = uip.getNetmask();
                gateway = uip.getGateway();
            } else {
                ip6 = uip.getIp();
                gateway6 = uip.getGateway();
                NormalIpRangeVO ipRangeVO = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, uip.getIpRangeUuid()).find();
                prefixLength = ipRangeVO.getPrefixLen();
                addressMode = ipRangeVO.getAddressMode();
            }
        }
        /* for virtual router, gateway ip is in the usedIpVO */
        if (inv.getUsedIps().size() == 0) {
            ip = inv.getIp();
            netmask = inv.getNetmask();
            gateway = inv.getGateway();
        }
        mac = inv.getMac();
    }

    public ApplianceVmNicTO() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public boolean isDefaultRoute() {
        return isDefaultRoute;
    }

    public void setDefaultRoute(boolean isDefaultRoute) {
        this.isDefaultRoute = isDefaultRoute;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getL2type() {
        return l2type;
    }

    public void setL2type(String l2type) {
        this.l2type = l2type;
    }

    public Integer getVni() {
        return vni;
    }

    public void setVni(Integer vni) {
        this.vni = vni;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public String getIp6() {
        return ip6;
    }

    public void setIp6(String ip6) {
        this.ip6 = ip6;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(Integer prefixLength) {
        this.prefixLength = prefixLength;
    }

    public String getGateway6() {
        return gateway6;
    }

    public void setGateway6(String gateway6) {
        this.gateway6 = gateway6;
    }

    public String getAddressMode() {
        return addressMode;
    }

    public void setAddressMode(String addressMode) {
        this.addressMode = addressMode;
    }
}
