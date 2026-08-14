package org.zstack.sdk;



public class BatchSyncVolumeSizeResult {
    public int successCount;
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    public int getSuccessCount() {
        return this.successCount;
    }

    public int failCount;
    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
    public int getFailCount() {
        return this.failCount;
    }

}
