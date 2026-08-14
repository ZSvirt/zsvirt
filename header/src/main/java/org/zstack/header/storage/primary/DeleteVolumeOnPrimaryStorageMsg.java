package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.ReplayableMessage;
import org.zstack.header.volume.VolumeInventory;

public class DeleteVolumeOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage, ReplayableMessage {
    private String uuid;
    private VolumeInventory volume;

    @Override
    public String getPrimaryStorageUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    @Override
    public String getResourceUuid() {
        return volume.getUuid();
    }

    @Override
    public Class getReplayableClass() {
        return DeleteVolumeOnPrimaryStorageMsg.class;
    }
}
