package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.HostKernelInterfaceInventory;

public class HostKernelInterfaceResult  {

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

    public HostKernelInterfaceInventory inventory;
    public void setInventory(HostKernelInterfaceInventory inventory) {
        this.inventory = inventory;
    }
    public HostKernelInterfaceInventory getInventory() {
        return this.inventory;
    }

}
