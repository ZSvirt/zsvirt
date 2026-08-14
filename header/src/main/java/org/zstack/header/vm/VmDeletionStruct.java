package org.zstack.header.vm;

import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;

/**
 * Created by xing5 on 2016/3/22.
 */
public class VmDeletionStruct {
    private VmInstanceInventory inventory;
    private VmInstanceDeletionPolicy deletionPolicy;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public VmInstanceDeletionPolicy getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(VmInstanceDeletionPolicy deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }
}
