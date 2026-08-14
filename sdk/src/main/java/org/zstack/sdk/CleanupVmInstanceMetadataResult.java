package org.zstack.sdk;



public class CleanupVmInstanceMetadataResult {
    public java.lang.Integer totalCleaned;
    public void setTotalCleaned(java.lang.Integer totalCleaned) {
        this.totalCleaned = totalCleaned;
    }
    public java.lang.Integer getTotalCleaned() {
        return this.totalCleaned;
    }

    public java.lang.Integer totalFailed;
    public void setTotalFailed(java.lang.Integer totalFailed) {
        this.totalFailed = totalFailed;
    }
    public java.lang.Integer getTotalFailed() {
        return this.totalFailed;
    }

    public java.util.List failedVmUuids;
    public void setFailedVmUuids(java.util.List failedVmUuids) {
        this.failedVmUuids = failedVmUuids;
    }
    public java.util.List getFailedVmUuids() {
        return this.failedVmUuids;
    }

}
