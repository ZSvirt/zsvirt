package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceReply extends AllocatePrimaryStorageReply {
    private String allocatedInstallUrl;

    public AllocatePrimaryStorageSpaceReply(PrimaryStorageInventory primaryStorageInventory) {
        super(primaryStorageInventory);
    }

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
    }
}
