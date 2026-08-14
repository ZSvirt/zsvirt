package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * Created by MaJin on 2021/3/18.
 */
public class CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeSnapshotInventory volumeSnapshot;
    private ImageInventory imageInventory;

    @Override
    public String getPrimaryStorageUuid() {
        return volumeSnapshot.getPrimaryStorageUuid();
    }

    public void setVolumeSnapshot(VolumeSnapshotInventory volumeSnapshot) {
        this.volumeSnapshot = volumeSnapshot;
    }

    public VolumeSnapshotInventory getVolumeSnapshot() {
        return volumeSnapshot;
    }

    public ImageInventory getImageInventory() {
        return imageInventory;
    }

    public void setImageInventory(ImageInventory imageInventory) {
        this.imageInventory = imageInventory;
    }

}
