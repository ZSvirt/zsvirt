package org.zstack.crypto.keyprovider.api;

import java.util.ArrayList;
import java.util.List;

public class RekeyProviderResult {
    private String providerUuid;
    private String providerName;
    private int totalRefCount;
    private int successRefCount;
    private int skippedRefCount;
    private int failedRefCount;
    private List<RekeySkippedResource> skippedResources = new ArrayList<>();
    private List<RekeyFailedResource> failedResources = new ArrayList<>();

    public static RekeyProviderResult __example__() {
        RekeyProviderResult result = new RekeyProviderResult();
        result.setProviderUuid("provider-uuid-1");
        result.setProviderName("provider-1");
        result.setTotalRefCount(3);
        result.setSuccessRefCount(1);
        result.setSkippedRefCount(1);
        result.setFailedRefCount(1);
        result.getSkippedResources().add(RekeySkippedResource.__example__());
        result.getFailedResources().add(RekeyFailedResource.__example__());
        return result;
    }

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public int getTotalRefCount() {
        return totalRefCount;
    }

    public void setTotalRefCount(int totalRefCount) {
        this.totalRefCount = totalRefCount;
    }

    public int getSuccessRefCount() {
        return successRefCount;
    }

    public void setSuccessRefCount(int successRefCount) {
        this.successRefCount = successRefCount;
    }

    public int getSkippedRefCount() {
        return skippedRefCount;
    }

    public void setSkippedRefCount(int skippedRefCount) {
        this.skippedRefCount = skippedRefCount;
    }

    public int getFailedRefCount() {
        return failedRefCount;
    }

    public void setFailedRefCount(int failedRefCount) {
        this.failedRefCount = failedRefCount;
    }

    public List<RekeySkippedResource> getSkippedResources() {
        return skippedResources;
    }

    public void setSkippedResources(List<RekeySkippedResource> skippedResources) {
        this.skippedResources = skippedResources == null ? new ArrayList<>() : skippedResources;
    }

    public List<RekeyFailedResource> getFailedResources() {
        return failedResources;
    }

    public void setFailedResources(List<RekeyFailedResource> failedResources) {
        this.failedResources = failedResources == null ? new ArrayList<>() : failedResources;
    }
}
