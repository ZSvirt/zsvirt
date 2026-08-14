package org.zstack.sdk;



public class CpuMemoryCapacityData  {

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

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

}
