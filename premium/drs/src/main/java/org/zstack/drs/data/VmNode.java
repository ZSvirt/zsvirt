package org.zstack.drs.data;

import java.io.Serializable;

/**
 * Created by lining on 2019/12/6.
 */
public class VmNode implements Serializable {
    public String uuid;

    private float usedCPUPercent; // [0, 100] Percentage of CPU used on the physical machine
    private float usedMemoryPercent; // [0, 100] Percentage of memory used on the physical machine
    private long usedPhysicalMemoryBit; // Occupies the memory capacity of the physical machine
    private float usedPhysicalCpuNum; // Occupies the CPU cores of the physical machine from top command

    private int cpuNum;
    private long memorySize;

    public VmNode(String vmUuid, float usedPhysicalCpuNum, float usedCPUPercent, long usedPhysicalMemoryBit, float usedMemoryPercent, int cpuNum, long memorySize) {
        this.uuid = vmUuid;
        this.usedPhysicalCpuNum = usedPhysicalCpuNum;
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

    public float getUsedPhysicalCpuNum() {
        return usedPhysicalCpuNum;
    }

    public void setUsedPhysicalCpuNum(float usedPhysicalCpuNum) {
        this.usedPhysicalCpuNum = usedPhysicalCpuNum;
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
