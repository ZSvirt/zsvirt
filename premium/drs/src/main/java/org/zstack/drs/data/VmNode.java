package org.zstack.drs.data;

import java.io.Serializable;

/**
 * Created by lining on 2019/12/6.
 */
public class VmNode implements Serializable {
    public String uuid;

    private float usedCPUPercent; // Percentage of CPU used on the physical machine
    private float usedMemoryPercent; // Percentage of memory used on the physical machine
    private long usedPhysicalMemoryBit; // Occupies the memory capacity of the physical machine

    private int cpuNum;
    private long memorySize;

    public VmNode(String vmUuid, float usedCPUPercent, long usedPhysicalMemoryBit, float usedMemoryPercent, int cpuNum, long memorySize) {
        this.uuid = vmUuid;
        this.usedCPUPercent = usedCPUPercent;
        this.usedPhysicalMemoryBit = usedPhysicalMemoryBit;
        this.usedMemoryPercent = usedMemoryPercent;

        this.cpuNum = cpuNum;
        this.memorySize = memorySize;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public float getUsedCPUPercent() {
        return usedCPUPercent;
    }

    public void setUsedCPUPercent(float usedCPUPercent) {
        this.usedCPUPercent = usedCPUPercent;
    }

    public long getUsedPhysicalMemoryBit() {
        return usedPhysicalMemoryBit;
    }

    public void setUsedPhysicalMemoryBit(long usedPhysicalMemoryBit) {
        this.usedPhysicalMemoryBit = usedPhysicalMemoryBit;
    }

    public float getUsedMemoryPercent() {
        return usedMemoryPercent;
    }

    private void setUsedMemoryPercent(float usedMemoryPercent) {
        this.usedMemoryPercent = usedMemoryPercent;
    }
}
