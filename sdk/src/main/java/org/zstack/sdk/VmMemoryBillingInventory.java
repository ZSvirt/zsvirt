package org.zstack.sdk;



public class VmMemoryBillingInventory extends org.zstack.sdk.BillingInventory {

    public long memorySize;
    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }
    public long getMemorySize() {
        return this.memorySize;
    }

}
