package org.zstack.header.allocator;

/**
 * Created by frank on 9/17/2015.
 */
public class HostCapacityStruct {
    private HostCapacityVO capacityVO;
    private long totalCpu;
    private long totalMemory;
    private long usedCpu;
    private long usedMemory;
    private int cpuNum;
    private int cpuSockets;
    private boolean init;

    public int getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(int cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public HostCapacityVO getCapacityVO() {
        return capacityVO;
    }

    public void setCapacityVO(HostCapacityVO capacityVO) {
        this.capacityVO = capacityVO;
    }

    public long getTotalCpu() {
        return totalCpu;
    }

    public void setTotalCpu(long totalCpu) {
        this.totalCpu = totalCpu;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getUsedCpu() {
        return usedCpu;
    }

    public void setUsedCpu(long usedCpu) {
        this.usedCpu = usedCpu;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }
}
