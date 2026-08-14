package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

public class InstantiateRootVolumeForRecoveryReply extends MessageReply {
    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
