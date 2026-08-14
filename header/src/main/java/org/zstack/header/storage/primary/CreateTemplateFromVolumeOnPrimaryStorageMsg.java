package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

public class CreateTemplateFromVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volumeInventory;
    private ImageInventory imageInventory;
    private String backupStorageUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return volumeInventory.getPrimaryStorageUuid();
    }

    public VolumeInventory getVolumeInventory() {
        return volumeInventory;
    }

    public void setVolumeInventory(VolumeInventory volumeInventory) {
        this.volumeInventory = volumeInventory;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public ImageInventory getImageInventory() {
        return imageInventory;
    }

    public void setImageInventory(ImageInventory imageInventory) {
        this.imageInventory = imageInventory;
    }
}
