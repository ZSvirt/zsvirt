package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.errorcode.ErrorCode;

public class HostKernelInterfaceResult {
    private ErrorCode error;
    private HostKernelInterfaceInventory inventory;

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }

    public HostKernelInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostKernelInterfaceInventory inventory) {
        this.inventory = inventory;
    }

    public static HostKernelInterfaceResult __example__() {
        HostKernelInterfaceResult result = new HostKernelInterfaceResult();
        result.setInventory(HostKernelInterfaceInventory.__example__());
        return result;
    }
}
