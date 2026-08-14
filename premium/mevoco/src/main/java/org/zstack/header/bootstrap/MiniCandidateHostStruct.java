package org.zstack.header.bootstrap;

import java.util.Map;

public class MiniCandidateHostStruct {
    public String hostName;
    public String ipv4Address;
    public String ipv6Address;
    public String ipv4Interface;
    public String ipv4CidrPrefix;
    public String ipv4InterfaceBond;
    public String ipv4Gateway;
    public String managementVip;
    public String ipmiIpv4Addr;
    public String ipmiIpv4Gateway;
    public String ipmiVlan;
    public String ipv6Interface;
    public String manufacturer;
    public String product;
    public String sn;

    public static MiniCandidateHostStruct valueOf(Map that) {
        MiniCandidateHostStruct r = new MiniCandidateHostStruct();
        r.setHostName((String)that.get("hostName"));
        r.setIpv4Address((String)that.get("ipv4Address"));
        r.setIpv6Address((String)that.get("ipv6Address"));
        r.setIpv4Interface((String)that.get("ipv4Interface"));
        r.setIpv6Interface((String)that.get("ipv6Interface"));
        r.setManufacturer((String)that.get("manufacturer"));
        r.setProduct((String)that.get("product"));
        r.setSn((String)that.get("sn"));
        r.setIpv4CidrPrefix((String)that.get("ipv4CidrPrefix"));
        r.setIpv4InterfaceBond((String)that.get("ipv4InterfaceBond"));
        r.setIpv4Gateway((String)that.get("ipv4Gateway"));
        r.setManagementVip((String)that.get("managementVip"));
        r.setIpmiIpv4Addr((String)that.get("ipmiIpv4Addr"));
        r.setIpmiIpv4Gateway((String)that.get("ipmiIpv4Gateway"));
        r.setIpmiVlan((String)that.get("ipmiVlan"));

        return r;
    }


    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpv4Address() {
        return ipv4Address;
    }

    public void setIpv4Address(String ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public String getIpv4Interface() {
        return ipv4Interface;
    }

    public void setIpv4Interface(String ipv4Interface) {
        this.ipv4Interface = ipv4Interface;
    }

    public String getIpv6Interface() {
        return ipv6Interface;
    }

    public void setIpv6Interface(String ipv6Interface) {
        this.ipv6Interface = ipv6Interface;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getIpv4CidrPrefix() {
        return ipv4CidrPrefix;
    }

    public void setIpv4CidrPrefix(String ipv4CidrPrefix) {
        this.ipv4CidrPrefix = ipv4CidrPrefix;
    }

    public String getIpv4InterfaceBond() {
        return ipv4InterfaceBond;
    }

    public void setIpv4InterfaceBond(String ipv4InterfaceBond) {
        this.ipv4InterfaceBond = ipv4InterfaceBond;
    }

    public String getIpv4Gateway() {
        return ipv4Gateway;
    }

    public void setIpv4Gateway(String ipv4Gateway) {
        this.ipv4Gateway = ipv4Gateway;
    }

    public String getManagementVip() {
        return managementVip;
    }

    public void setManagementVip(String managementVip) {
        this.managementVip = managementVip;
    }

    public String getIpmiIpv4Addr() {
        return ipmiIpv4Addr;
    }

    public void setIpmiIpv4Addr(String ipmiIpv4Addr) {
        this.ipmiIpv4Addr = ipmiIpv4Addr;
    }

    public String getIpmiIpv4Gateway() {
        return ipmiIpv4Gateway;
    }

    public void setIpmiIpv4Gateway(String ipmiIpv4Gateway) {
        this.ipmiIpv4Gateway = ipmiIpv4Gateway;
    }

    public String getIpmiVlan() {
        return ipmiVlan;
    }

    public void setIpmiVlan(String ipmiVlan) {
        this.ipmiVlan = ipmiVlan;
    }
}
