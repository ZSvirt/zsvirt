package org.zstack.sdk;



public class CephBackupStorageInventory extends org.zstack.sdk.BackupStorageInventory {

    public java.util.List mons;
    public void setMons(java.util.List mons) {
        this.mons = mons;
    }
    public java.util.List getMons() {
        return this.mons;
    }

    public java.lang.String fsid;
    public void setFsid(java.lang.String fsid) {
        this.fsid = fsid;
    }
    public java.lang.String getFsid() {
        return this.fsid;
    }

    public java.lang.String poolName;
    public void setPoolName(java.lang.String poolName) {
        this.poolName = poolName;
    }
    public java.lang.String getPoolName() {
        return this.poolName;
    }

    public java.lang.Long poolAvailableCapacity;
    public void setPoolAvailableCapacity(java.lang.Long poolAvailableCapacity) {
        this.poolAvailableCapacity = poolAvailableCapacity;
    }
    public java.lang.Long getPoolAvailableCapacity() {
        return this.poolAvailableCapacity;
    }

    public java.lang.Long poolUsedCapacity;
    public void setPoolUsedCapacity(java.lang.Long poolUsedCapacity) {
        this.poolUsedCapacity = poolUsedCapacity;
    }
    public java.lang.Long getPoolUsedCapacity() {
        return this.poolUsedCapacity;
    }

    public java.lang.Integer poolReplicatedSize;
    public void setPoolReplicatedSize(java.lang.Integer poolReplicatedSize) {
        this.poolReplicatedSize = poolReplicatedSize;
    }
    public java.lang.Integer getPoolReplicatedSize() {
        return this.poolReplicatedSize;
    }

    public java.lang.Float poolDiskUtilization;
    public void setPoolDiskUtilization(java.lang.Float poolDiskUtilization) {
        this.poolDiskUtilization = poolDiskUtilization;
    }
    public java.lang.Float getPoolDiskUtilization() {
        return this.poolDiskUtilization;
    }

    public java.lang.String poolSecurityPolicy;
    public void setPoolSecurityPolicy(java.lang.String poolSecurityPolicy) {
        this.poolSecurityPolicy = poolSecurityPolicy;
    }
    public java.lang.String getPoolSecurityPolicy() {
        return this.poolSecurityPolicy;
    }

}
