package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.message.ReplayableMessage;
import org.zstack.header.volume.InstantiateVolumeMsg;

public class InstantiateRootVolumeForRecoveryMsg extends InstantiateVolumeMsg implements ReplayableMessage {
    private ImageBackupStorageRefInventory selectedBackupStorage;

    public ImageBackupStorageRefInventory getSelectedBackupStorage() {
        return selectedBackupStorage;
    }

    public void setSelectedBackupStorage(ImageBackupStorageRefInventory selectedBackupStorage) {
        this.selectedBackupStorage = selectedBackupStorage;
    }

    @Override
    public String getResourceUuid() {
        return this.getVolumeUuid();
    }

    @Override
    public Class getReplayableClass() {
        return InstantiateRootVolumeForRecoveryMsg.class;
    }
}
