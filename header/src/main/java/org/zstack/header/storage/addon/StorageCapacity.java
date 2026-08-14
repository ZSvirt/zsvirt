package org.zstack.header.storage.addon;

public class StorageCapacity {
    private long totalCapacity;
    private long availableCapacity;
    private StorageHealthy healthy;

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public StorageHealthy getHealthy() {
        return healthy;
    }

    public void setHealthy(StorageHealthy healthy) {
        this.healthy = healthy;
    }
}
