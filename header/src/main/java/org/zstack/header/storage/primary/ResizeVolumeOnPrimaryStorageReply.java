package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by kayo on 2017/9/12.
 */
public class ResizeVolumeOnPrimaryStorageReply extends MessageReply {
    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
