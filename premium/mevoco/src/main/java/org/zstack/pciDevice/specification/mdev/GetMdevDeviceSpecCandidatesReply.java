package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
public class GetMdevDeviceSpecCandidatesReply extends MessageReply {
    private List<MdevDeviceSpecInventory> inventories;

    public List<MdevDeviceSpecInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MdevDeviceSpecInventory> inventories) {
        this.inventories = inventories;
    }
}
