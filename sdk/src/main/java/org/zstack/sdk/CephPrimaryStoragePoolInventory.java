package org.zstack.sdk;



public class CephPrimaryStoragePoolInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public java.lang.String poolName;
    public void setPoolName(java.lang.String poolName) {
        this.poolName = poolName;
    }
    public java.lang.String getPoolName() {
        return this.poolName;
    }

    public java.lang.String aliasName;
    public void setAliasName(java.lang.String aliasName) {
        this.aliasName = aliasName;
    }
    public java.lang.String getAliasName() {
        return this.aliasName;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
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

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.Long availableCapacity;
    public void setAvailableCapacity(java.lang.Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public java.lang.Long getAvailableCapacity() {
        return this.availableCapacity;
    }

    public java.lang.Long usedCapacity;
    public void setUsedCapacity(java.lang.Long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }
    public java.lang.Long getUsedCapacity() {
        return this.usedCapacity;
    }

    public java.lang.Long totalCapacity;
    public void setTotalCapacity(java.lang.Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public java.lang.Long getTotalCapacity() {
        return this.totalCapacity;
    }

    public java.lang.String securityPolicy;
    public void setSecurityPolicy(java.lang.String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }
    public java.lang.String getSecurityPolicy() {
        return this.securityPolicy;
    }

    public java.lang.Integer replicatedSize;
    public void setReplicatedSize(java.lang.Integer replicatedSize) {
        this.replicatedSize = replicatedSize;
    }
    public java.lang.Integer getReplicatedSize() {
        return this.replicatedSize;
    }

    public java.lang.Float diskUtilization;
    public void setDiskUtilization(java.lang.Float diskUtilization) {
        this.diskUtilization = diskUtilization;
    }
    public java.lang.Float getDiskUtilization() {
        return this.diskUtilization;
    }

}
