package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2022/7/3 16:52
 */
public class SetVolumeQosReply extends MessageReply {
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
