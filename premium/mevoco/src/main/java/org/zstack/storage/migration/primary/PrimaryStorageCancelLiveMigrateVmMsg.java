package org.zstack.storage.migration.primary;


import org.zstack.header.message.CancelMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

public class PrimaryStorageCancelLiveMigrateVmMsg  extends CancelMessage implements StorageMigrationMessage {
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
