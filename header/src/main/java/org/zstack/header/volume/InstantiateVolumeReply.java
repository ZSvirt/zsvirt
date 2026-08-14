package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/8/22.
 */
public class InstantiateVolumeReply extends MessageReply {
    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
