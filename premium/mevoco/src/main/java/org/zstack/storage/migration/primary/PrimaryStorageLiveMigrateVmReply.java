package org.zstack.storage.migration.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;

public class PrimaryStorageLiveMigrateVmReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
