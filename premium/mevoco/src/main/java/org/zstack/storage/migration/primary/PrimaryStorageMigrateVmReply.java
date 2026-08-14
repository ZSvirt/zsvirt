package org.zstack.storage.migration.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by GuoYi on 12/7/17.
 */
public class PrimaryStorageMigrateVmReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
