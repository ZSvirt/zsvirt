package org.zstack.sdk;



public class GetPrimaryStorageCapacityResult {
    public long totalCapacity;
    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public long getTotalCapacity() {
        return this.totalCapacity;
    }

    public long availableCapacity;
    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    public long getAvailableCapacity() {
        return this.availableCapacity;
    }

    public long totalPhysicalCapacity;
    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }
    public long getTotalPhysicalCapacity() {
        return this.totalPhysicalCapacity;
    }

    public long availablePhysicalCapacity;
    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }
    public long getAvailablePhysicalCapacity() {
        return this.availablePhysicalCapacity;
    }

}
