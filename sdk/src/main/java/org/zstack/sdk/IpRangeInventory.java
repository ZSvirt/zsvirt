package org.zstack.sdk;

import org.zstack.sdk.IpRangeType;

public class IpRangeInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String l3NetworkUuid;
    public void setL3NetworkUuid(java.lang.String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public java.lang.String getL3NetworkUuid() {
        return this.l3NetworkUuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String startIp;
    public void setStartIp(java.lang.String startIp) {
        this.startIp = startIp;
    }
    public java.lang.String getStartIp() {
        return this.startIp;
    }

    public java.lang.String endIp;
    public void setEndIp(java.lang.String endIp) {
        this.endIp = endIp;
    }
    public java.lang.String getEndIp() {
        return this.endIp;
    }

    public java.lang.String netmask;
    public void setNetmask(java.lang.String netmask) {
        this.netmask = netmask;
    }
    public java.lang.String getNetmask() {
        return this.netmask;
    }

    public java.lang.String gateway;
    public void setGateway(java.lang.String gateway) {
        this.gateway = gateway;
    }
    public java.lang.String getGateway() {
        return this.gateway;
    }

    public java.lang.String networkCidr;
    public void setNetworkCidr(java.lang.String networkCidr) {
        this.networkCidr = networkCidr;
    }
    public java.lang.String getNetworkCidr() {
        return this.networkCidr;
    }

    public java.lang.Integer ipVersion;
    public void setIpVersion(java.lang.Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
    public java.lang.Integer getIpVersion() {
        return this.ipVersion;
    }

    public java.lang.String addressMode;
    public void setAddressMode(java.lang.String addressMode) {
        this.addressMode = addressMode;
    }
    public java.lang.String getAddressMode() {
        return this.addressMode;
    }

    public java.lang.Integer prefixLen;
    public void setPrefixLen(java.lang.Integer prefixLen) {
        this.prefixLen = prefixLen;
    }
    public java.lang.Integer getPrefixLen() {
        return this.prefixLen;
    }

    public IpRangeType ipRangeType;
    public void setIpRangeType(IpRangeType ipRangeType) {
        this.ipRangeType = ipRangeType;
    }
    public IpRangeType getIpRangeType() {
        return this.ipRangeType;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
