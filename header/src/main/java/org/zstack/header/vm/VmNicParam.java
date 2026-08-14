package org.zstack.header.vm;

import java.io.Serializable;
import java.util.List;

public class VmNicParam implements Serializable {
    private String l3NetworkUuid;

    private String mac;

    private String ip;

    private String netmask;

    private String gateway;

    private List<String> dnsList;

    private String ip6;

    private Integer ipv6Prefix;

    private String ipv6Gateway;

    private List<String> dns6List;

    private String metaData;

    private String driverType;

    private String state = VmNicState.enable.toString();

    private Long outboundBandwidth;

    private Long inboundBandwidth;

    private Integer multiQueueNum;

    private String vfParentUuid;

    private boolean isDefaultNic = false;

    private List<String> sgUuids;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
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

    public List<String> getDnsList() {
        return dnsList;
    }

    public void setDnsList(List<String> dnsList) {
        this.dnsList = dnsList;
    }

    public String getIp6() {
        return ip6;
    }

    public void setIp6(String ip6) {
        this.ip6 = ip6;
    }

    public Integer getIpv6Prefix() {
        return ipv6Prefix;
    }

    public void setIpv6Prefix(Integer ipv6Prefix) {
        this.ipv6Prefix = ipv6Prefix;
    }

    public String getIpv6Gateway() {
        return ipv6Gateway;
    }

    public void setIpv6Gateway(String ipv6Gateway) {
        this.ipv6Gateway = ipv6Gateway;
    }

    public List<String> getDns6List() {
        return dns6List;
    }

    public void setDns6List(List<String> dns6List) {
        this.dns6List = dns6List;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public Integer getMultiQueueNum() {
        return multiQueueNum;
    }

    public void setMultiQueueNum(Integer multiQueueNum) {
        this.multiQueueNum = multiQueueNum;
    }

    public Boolean isSriovEnabled() {
        return VmNicConstant.NIC_DRIVER_TYPE_SR_IOV.equals(driverType);
    }

    public String getVfParentUuid() {
        return vfParentUuid;
    }

    public void setVfParentUuid(String vfParentUuid) {
        this.vfParentUuid = vfParentUuid;
    }

    public boolean isDefaultNic() {
        return isDefaultNic;
    }

    public void setDefaultNic(boolean defaultNic) {
        this.isDefaultNic = defaultNic;
    }

    public List<String> getSgUuids() {
        return sgUuids;
    }

    public void setSgUuids(List<String> sgUuids) {
        this.sgUuids = sgUuids;
    }
}
