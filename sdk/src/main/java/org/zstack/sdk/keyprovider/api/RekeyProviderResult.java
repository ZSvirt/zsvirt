package org.zstack.sdk.keyprovider.api;



public class RekeyProviderResult  {

    public java.lang.String providerUuid;
    public void setProviderUuid(java.lang.String providerUuid) {
        this.providerUuid = providerUuid;
    }
    public java.lang.String getProviderUuid() {
        return this.providerUuid;
    }

    public java.lang.String providerName;
    public void setProviderName(java.lang.String providerName) {
        this.providerName = providerName;
    }
    public java.lang.String getProviderName() {
        return this.providerName;
    }

    public int totalRefCount;
    public void setTotalRefCount(int totalRefCount) {
        this.totalRefCount = totalRefCount;
    }
    public int getTotalRefCount() {
        return this.totalRefCount;
    }

    public int successRefCount;
    public void setSuccessRefCount(int successRefCount) {
        this.successRefCount = successRefCount;
    }
    public int getSuccessRefCount() {
        return this.successRefCount;
    }

    public int skippedRefCount;
    public void setSkippedRefCount(int skippedRefCount) {
        this.skippedRefCount = skippedRefCount;
    }
    public int getSkippedRefCount() {
        return this.skippedRefCount;
    }

    public int failedRefCount;
    public void setFailedRefCount(int failedRefCount) {
        this.failedRefCount = failedRefCount;
    }
    public int getFailedRefCount() {
        return this.failedRefCount;
    }

    public java.util.List skippedResources;
    public void setSkippedResources(java.util.List skippedResources) {
        this.skippedResources = skippedResources;
    }
    public java.util.List getSkippedResources() {
        return this.skippedResources;
    }

    public java.util.List failedResources;
    public void setFailedResources(java.util.List failedResources) {
        this.failedResources = failedResources;
    }
    public java.util.List getFailedResources() {
        return this.failedResources;
    }

}
