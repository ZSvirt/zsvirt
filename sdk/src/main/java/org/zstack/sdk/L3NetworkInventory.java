package org.zstack.sdk;



public class L3NetworkInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
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

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
    }

    public java.lang.String l2NetworkUuid;
    public void setL2NetworkUuid(java.lang.String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
    public java.lang.String getL2NetworkUuid() {
        return this.l2NetworkUuid;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String dnsDomain;
    public void setDnsDomain(java.lang.String dnsDomain) {
        this.dnsDomain = dnsDomain;
    }
    public java.lang.String getDnsDomain() {
        return this.dnsDomain;
    }

    public java.lang.Boolean system;
    public void setSystem(java.lang.Boolean system) {
        this.system = system;
    }
    public java.lang.Boolean getSystem() {
        return this.system;
    }

    public java.lang.String category;
    public void setCategory(java.lang.String category) {
        this.category = category;
    }
    public java.lang.String getCategory() {
        return this.category;
    }

    @Deprecated
    public java.lang.Integer ipVersion;
    public void setIpVersion(java.lang.Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
    public java.lang.Integer getIpVersion() {
        return this.ipVersion;
    }

    public java.lang.Boolean enableIPAM;
    public void setEnableIPAM(java.lang.Boolean enableIPAM) {
        this.enableIPAM = enableIPAM;
    }
    public java.lang.Boolean getEnableIPAM() {
        return this.enableIPAM;
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

    public java.util.List dns;
    public void setDns(java.util.List dns) {
        this.dns = dns;
    }
    public java.util.List getDns() {
        return this.dns;
    }

    public java.util.List ipRanges;
    public void setIpRanges(java.util.List ipRanges) {
        this.ipRanges = ipRanges;
    }
    public java.util.List getIpRanges() {
        return this.ipRanges;
    }

    public java.util.List networkServices;
    public void setNetworkServices(java.util.List networkServices) {
        this.networkServices = networkServices;
    }
    public java.util.List getNetworkServices() {
        return this.networkServices;
    }

    public java.util.List hostRoute;
    public void setHostRoute(java.util.List hostRoute) {
        this.hostRoute = hostRoute;
    }
    public java.util.List getHostRoute() {
        return this.hostRoute;
    }

    public java.util.List reservedIpRanges;
    public void setReservedIpRanges(java.util.List reservedIpRanges) {
        this.reservedIpRanges = reservedIpRanges;
    }
    public java.util.List getReservedIpRanges() {
        return this.reservedIpRanges;
    }

}
