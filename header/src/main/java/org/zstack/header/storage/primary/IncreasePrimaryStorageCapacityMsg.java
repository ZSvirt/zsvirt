package org.zstack.header.storage.primary;

import org.zstack.header.message.Message;

@Deprecated
public class IncreasePrimaryStorageCapacityMsg extends Message {
    private String primaryStorageUuid;
    private long diskSize;
    private boolean noOverProvisioning;

    public boolean isNoOverProvisioning() {
        return noOverProvisioning;
    }

    public void setNoOverProvisioning(boolean noOverProvisioning) {
        this.noOverProvisioning = noOverProvisioning;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }
}
