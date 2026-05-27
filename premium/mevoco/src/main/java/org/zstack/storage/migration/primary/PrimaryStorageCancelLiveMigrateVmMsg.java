package org.zstack.storage.migration.primary;


import org.zstack.header.message.CancelMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

import java.util.Collections;
import java.util.List;

public class PrimaryStorageCancelLiveMigrateVmMsg  extends CancelMessage implements StorageMigrationMessage {
    private String vmInstanceUuid;

    private List<VolumeMigrationAO> volumeMigrationAOs;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<VolumeMigrationAO> getVolumeMigrationAOs() {
        return volumeMigrationAOs == null ? Collections.emptyList() : volumeMigrationAOs;
    }

    public void setVolumeMigrationAOs(List<VolumeMigrationAO> volumeMigrationAOs) {
        this.volumeMigrationAOs = volumeMigrationAOs;
    }
}
