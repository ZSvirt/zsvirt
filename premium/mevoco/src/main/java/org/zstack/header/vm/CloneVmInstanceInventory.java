package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by xing5 on 2017/2/17.
 */
public class CloneVmInstanceInventory {
    private ErrorCode error;
    private VmInstanceInventory inventory;

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
