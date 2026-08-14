package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DhcpStruct implements Serializable {
    private int    ipVersion;
    private String ip;
    private String ip6;
    private String mac;
    private String netmask;
    private String gateway;
    private String gateway6;
    private String hostname;
    private L3NetworkInventory l3Network;
    private boolean isDefaultL3Network;
    private String dnsDomain;
    private Integer mtu;
    private String  raMode;
    private boolean enableRa;
    private String  firstIp;
    private String  endIP;
    private Integer prefixLength;
    private String  vmUuid;
    private String  nicType;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(String.format("version: %d,", ipVersion));
        sb.append(String.format("ip: %s,", ip));
        sb.append(String.format("ip6: %s,", ip6));
        sb.append(String.format("mac: %s,", mac));
        sb.append(String.format("netmask: %s,", netmask));
        sb.append(String.format("gateway: %s,", gateway));
        sb.append(String.format("gateway6: %s,", gateway6));
        sb.append(String.format("hostname: %s,", hostname));
        sb.append(String.format("l3NetworkUuid: %s,", l3Network.getUuid()));
        sb.append(String.format("dnsDomain: %s", dnsDomain));
        sb.append(String.format("isDefaultL3Network: %s", isDefaultL3Network));
        sb.append(String.format("mtu: %s", mtu));
        sb.append(String.format("raMode: %s", raMode));
        sb.append(String.format("firstIp: %s", firstIp));
        sb.append(String.format("endIP: %s", endIP));
        sb.append(String.format("prefixLength: %s", prefixLength));
        sb.append("]");
        return sb.toString();
    }

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String domain) {
        this.dnsDomain = domain;
    }

    public boolean isDefaultL3Network() {
        return isDefaultL3Network;
    }

    public void setDefaultL3Network(boolean isDefaultL3Network) {
        this.isDefaultL3Network = isDefaultL3Network;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public int getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(int ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getRaMode() {
        return raMode;
    }

    public void setRaMode(String raMode) {
        this.raMode = raMode;
    }

    public boolean isEnableRa() {
        return enableRa;
    }

    public void setEnableRa(boolean enableRa) {
        this.enableRa = enableRa;
    }

    public String getFirstIp() {
        return firstIp;
    }

    public void setFirstIp(String firstIp) {
        this.firstIp = firstIp;
    }

    public String getEndIP() {
        return endIP;
    }

    public void setEndIP(String endIP) {
        this.endIP = endIP;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(Integer prefixLength) {
        this.prefixLength = prefixLength;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getIp6() {
        return ip6;
    }

    public void setIp6(String ip6) {
        this.ip6 = ip6;
    }

    public String getGateway6() {
        return gateway6;
    }

    public void setGateway6(String gateway6) {
        this.gateway6 = gateway6;
    }

    public String getNicType() {
        return nicType;
    }

    public void setNicType(String nicType) {
        this.nicType = nicType;
    }
}
