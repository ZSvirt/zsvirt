package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageAllocationPurpose;

public class AllocateSpaceSpec {
    private long size;
    private boolean dryRun;
    private String requiredUrl;

    private PrimaryStorageAllocationPurpose purpose;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getRequiredUrl() {
        return requiredUrl;
    }

    public void setRequiredUrl(String requiredUrl) {
        this.requiredUrl = requiredUrl;
    }

    public PrimaryStorageAllocationPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(PrimaryStorageAllocationPurpose purpose) {
        this.purpose = purpose;
    }
}
