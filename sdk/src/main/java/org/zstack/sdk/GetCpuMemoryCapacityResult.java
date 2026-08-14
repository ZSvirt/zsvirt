package org.zstack.sdk;



public class GetCpuMemoryCapacityResult {
    public long totalCpu;
    public void setTotalCpu(long totalCpu) {
        this.totalCpu = totalCpu;
    }
    public long getTotalCpu() {
        return this.totalCpu;
    }

    public long availableCpu;
    public void setAvailableCpu(long availableCpu) {
        this.availableCpu = availableCpu;
    }
    public long getAvailableCpu() {
        return this.availableCpu;
    }

    public long totalMemory;
    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }
    public long getTotalMemory() {
        return this.totalMemory;
    }

    public long availableMemory;
    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }
    public long getAvailableMemory() {
        return this.availableMemory;
    }

    public long managedCpuNum;
    public void setManagedCpuNum(long managedCpuNum) {
        this.managedCpuNum = managedCpuNum;
    }
    public long getManagedCpuNum() {
        return this.managedCpuNum;
    }

    public java.util.List capacityData;
    public void setCapacityData(java.util.List capacityData) {
        this.capacityData = capacityData;
    }
    public java.util.List getCapacityData() {
        return this.capacityData;
    }

    public java.lang.String resourceType;
    public void setResourceType(java.lang.String resourceType) {
        this.resourceType = resourceType;
    }
    public java.lang.String getResourceType() {
        return this.resourceType;
    }

}
