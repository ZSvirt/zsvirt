package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/8/22.
 */
public class InstantiateRootVolumeReply extends NeedReplyMessage {
    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
