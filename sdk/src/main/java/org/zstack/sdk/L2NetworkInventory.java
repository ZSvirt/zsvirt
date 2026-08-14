package org.zstack.sdk;



public class L2NetworkInventory  {

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

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
    }

    public java.lang.String physicalInterface;
    public void setPhysicalInterface(java.lang.String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }
    public java.lang.String getPhysicalInterface() {
        return this.physicalInterface;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String vSwitchType;
    public void setVSwitchType(java.lang.String vSwitchType) {
        this.vSwitchType = vSwitchType;
    }
    public java.lang.String getVSwitchType() {
        return this.vSwitchType;
    }

    public java.lang.Integer virtualNetworkId;
    public void setVirtualNetworkId(java.lang.Integer virtualNetworkId) {
        this.virtualNetworkId = virtualNetworkId;
    }
    public java.lang.Integer getVirtualNetworkId() {
        return this.virtualNetworkId;
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

    public java.util.List attachedClusterUuids;
    public void setAttachedClusterUuids(java.util.List attachedClusterUuids) {
        this.attachedClusterUuids = attachedClusterUuids;
    }
    public java.util.List getAttachedClusterUuids() {
        return this.attachedClusterUuids;
    }

    public java.util.List attachedHostRefs;
    public void setAttachedHostRefs(java.util.List attachedHostRefs) {
        this.attachedHostRefs = attachedHostRefs;
    }
    public java.util.List getAttachedHostRefs() {
        return this.attachedHostRefs;
    }

}
