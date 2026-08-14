package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;

public class CheckChangeVolumeTypeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeInventory volume;
    private VolumeType targetType;

    @Override
    public String getPrimaryStorageUuid() {
        return volume.getPrimaryStorageUuid();
    }

    public VolumeType getTargetType() {
        return targetType;
    }

    public void setTargetType(VolumeType targetType) {
        this.targetType = targetType;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
