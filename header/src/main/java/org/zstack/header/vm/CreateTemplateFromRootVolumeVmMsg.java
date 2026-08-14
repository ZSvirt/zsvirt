package org.zstack.header.vm;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

public class CreateTemplateFromRootVolumeVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private ImageInventory imageInventory;
    private VolumeInventory rootVolumeInventory;
    private String backupStorageUuid;

    public VolumeInventory getRootVolumeInventory() {
        return rootVolumeInventory;
    }

    public void setRootVolumeInventory(VolumeInventory rootVolumeInventory) {
        this.rootVolumeInventory = rootVolumeInventory;
    }

    @Override
    public String getVmInstanceUuid() {
        return rootVolumeInventory.getVmInstanceUuid();
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
