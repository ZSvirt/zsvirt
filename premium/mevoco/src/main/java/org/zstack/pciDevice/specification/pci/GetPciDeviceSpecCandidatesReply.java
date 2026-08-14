package org.zstack.pciDevice.specification.pci;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
public class GetPciDeviceSpecCandidatesReply extends MessageReply {
    private List<PciDeviceSpecInventory> inventories;

    public List<PciDeviceSpecInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceSpecInventory> inventories) {
        this.inventories = inventories;
    }
}
