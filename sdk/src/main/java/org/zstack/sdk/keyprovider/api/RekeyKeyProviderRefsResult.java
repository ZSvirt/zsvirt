package org.zstack.sdk.keyprovider.api;



public class RekeyKeyProviderRefsResult {
    public int totalCount;
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    public int getTotalCount() {
        return this.totalCount;
    }

    public int successCount;
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    public int getSuccessCount() {
        return this.successCount;
    }

    public int skippedCount;
    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
    public int getSkippedCount() {
        return this.skippedCount;
    }

    public int failedCount;
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
    public int getFailedCount() {
        return this.failedCount;
    }

    public java.util.List providerResults;
    public void setProviderResults(java.util.List providerResults) {
        this.providerResults = providerResults;
    }
    public java.util.List getProviderResults() {
        return this.providerResults;
    }

}
