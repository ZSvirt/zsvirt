package org.zstack.pciDevice.message.virtual.vfio_mdev;

import org.zstack.header.message.MessageReply;
import org.zstack.pciDevice.entity.virtual.vfio_mdev.MdevDeviceInventory;

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
