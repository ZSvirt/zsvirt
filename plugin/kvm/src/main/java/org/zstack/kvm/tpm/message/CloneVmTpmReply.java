package org.zstack.kvm.tpm.message;

import org.zstack.header.message.MessageReply;
import org.zstack.header.tpm.entity.TpmInventory;

import java.util.List;

public class CloneVmTpmReply extends MessageReply {
    private List<TpmInventory> inventories;

    public List<TpmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TpmInventory> inventories) {
        this.inventories = inventories;
    }
}
