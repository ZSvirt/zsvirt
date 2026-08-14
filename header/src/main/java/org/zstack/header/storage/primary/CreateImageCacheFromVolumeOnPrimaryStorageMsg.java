package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by MaJin on 2020/9/14.
 */
public class CreateImageCacheFromVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volumeInventory;
    private ImageInventory imageInventory;

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

    public ImageInventory getImageInventory() {
        return imageInventory;
    }

    public void setImageInventory(ImageInventory imageInventory) {
        this.imageInventory = imageInventory;
    }
}
