package org.zstack.sdk;



public class VmMemorySpendingDetails extends org.zstack.sdk.VmSpendingDetails {

    public long memorySize;
    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }
    public long getMemorySize() {
        return this.memorySize;
    }

}
