package org.zstack.sdk;



public class BaremetalPxeServerInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
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

    public java.lang.String hostname;
    public void setHostname(java.lang.String hostname) {
        this.hostname = hostname;
    }
    public java.lang.String getHostname() {
        return this.hostname;
    }

    public java.lang.String sshUsername;
    public void setSshUsername(java.lang.String sshUsername) {
        this.sshUsername = sshUsername;
    }
    public java.lang.String getSshUsername() {
        return this.sshUsername;
    }

    public java.lang.String sshPassword;
    public void setSshPassword(java.lang.String sshPassword) {
        this.sshPassword = sshPassword;
    }
    public java.lang.String getSshPassword() {
        return this.sshPassword;
    }

    public java.lang.Integer sshPort;
    public void setSshPort(java.lang.Integer sshPort) {
        this.sshPort = sshPort;
    }
    public java.lang.Integer getSshPort() {
        return this.sshPort;
    }

    public java.lang.String storagePath;
    public void setStoragePath(java.lang.String storagePath) {
        this.storagePath = storagePath;
    }
    public java.lang.String getStoragePath() {
        return this.storagePath;
    }

    public java.lang.String dhcpInterface;
    public void setDhcpInterface(java.lang.String dhcpInterface) {
        this.dhcpInterface = dhcpInterface;
    }
    public java.lang.String getDhcpInterface() {
        return this.dhcpInterface;
    }

    public java.lang.String dhcpInterfaceAddress;
    public void setDhcpInterfaceAddress(java.lang.String dhcpInterfaceAddress) {
        this.dhcpInterfaceAddress = dhcpInterfaceAddress;
    }
    public java.lang.String getDhcpInterfaceAddress() {
        return this.dhcpInterfaceAddress;
    }

    public java.lang.String dhcpRangeBegin;
    public void setDhcpRangeBegin(java.lang.String dhcpRangeBegin) {
        this.dhcpRangeBegin = dhcpRangeBegin;
    }
    public java.lang.String getDhcpRangeBegin() {
        return this.dhcpRangeBegin;
    }

    public java.lang.String dhcpRangeEnd;
    public void setDhcpRangeEnd(java.lang.String dhcpRangeEnd) {
        this.dhcpRangeEnd = dhcpRangeEnd;
    }
    public java.lang.String getDhcpRangeEnd() {
        return this.dhcpRangeEnd;
    }

    public java.lang.String dhcpRangeNetmask;
    public void setDhcpRangeNetmask(java.lang.String dhcpRangeNetmask) {
        this.dhcpRangeNetmask = dhcpRangeNetmask;
    }
    public java.lang.String getDhcpRangeNetmask() {
        return this.dhcpRangeNetmask;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
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

    public java.lang.Long totalCapacity;
    public void setTotalCapacity(java.lang.Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public java.lang.Long getTotalCapacity() {
        return this.totalCapacity;
    }

    public java.lang.Long availableCapacity;
    public void setAvailableCapacity(java.lang.Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public java.lang.Long getAvailableCapacity() {
        return this.availableCapacity;
    }

    public java.util.List attachedClusterUuids;
    public void setAttachedClusterUuids(java.util.List attachedClusterUuids) {
        this.attachedClusterUuids = attachedClusterUuids;
    }
    public java.util.List getAttachedClusterUuids() {
        return this.attachedClusterUuids;
    }

}
