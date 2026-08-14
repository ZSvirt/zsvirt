package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2019-05-25.
 */
public class DetachMdevDeviceReply extends MessageReply {
    private MdevDeviceInventory inventory;

    public MdevDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(MdevDeviceInventory inventory) {
        this.inventory = inventory;
    }
}
