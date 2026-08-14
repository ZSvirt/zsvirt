package org.zstack.sdk.zmigrate.api;



public class GetZMigrateInfosResult {
    public java.lang.String zmigrateVmInstanceStatus;
    public void setZmigrateVmInstanceStatus(java.lang.String zmigrateVmInstanceStatus) {
        this.zmigrateVmInstanceStatus = zmigrateVmInstanceStatus;
    }
    public java.lang.String getZmigrateVmInstanceStatus() {
        return this.zmigrateVmInstanceStatus;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public long platformsCount;
    public void setPlatformsCount(long platformsCount) {
        this.platformsCount = platformsCount;
    }
    public long getPlatformsCount() {
        return this.platformsCount;
    }

    public long gatewaysCount;
    public void setGatewaysCount(long gatewaysCount) {
        this.gatewaysCount = gatewaysCount;
    }
    public long getGatewaysCount() {
        return this.gatewaysCount;
    }

    public long migrateJobsCount;
    public void setMigrateJobsCount(long migrateJobsCount) {
        this.migrateJobsCount = migrateJobsCount;
    }
    public long getMigrateJobsCount() {
        return this.migrateJobsCount;
    }

    public long zmigrateStartTime;
    public void setZmigrateStartTime(long zmigrateStartTime) {
        this.zmigrateStartTime = zmigrateStartTime;
    }
    public long getZmigrateStartTime() {
        return this.zmigrateStartTime;
    }

    public boolean vddkUploaded;
    public void setVddkUploaded(boolean vddkUploaded) {
        this.vddkUploaded = vddkUploaded;
    }
    public boolean getVddkUploaded() {
        return this.vddkUploaded;
    }

}
