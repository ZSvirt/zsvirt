package org.zstack.billing;

public class DiskOfferingAllocatePoolNamesConfig {
    private PrimaryStorageAllocatePoolNamesConfig primaryStorage;

    public PrimaryStorageAllocatePoolNamesConfig getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(PrimaryStorageAllocatePoolNamesConfig primaryStorage) {
        this.primaryStorage = primaryStorage;
    }
}
