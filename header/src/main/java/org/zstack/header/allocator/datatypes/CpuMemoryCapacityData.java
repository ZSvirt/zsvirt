package org.zstack.header.allocator.datatypes;

/**
 * Created by Qi Le on 2020/4/23
 */
public class CpuMemoryCapacityData {
    private String resourceUuid;
    private long totalCpu;
    private long availableCpu;
    private long totalMemory;
    private long availableMemory;
    private long managedCpuNum;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public long getTotalCpu() {
        return totalCpu;
    }

    public void setTotalCpu(long totalCpu) {
        this.totalCpu = totalCpu;
    }

    public long getAvailableCpu() {
        return availableCpu;
    }

    public void setAvailableCpu(long availableCpu) {
        this.availableCpu = availableCpu;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public long getManagedCpuNum() {
        return managedCpuNum;
    }

    public void setManagedCpuNum(long managedCpuNum) {
        this.managedCpuNum = managedCpuNum;
    }
}
