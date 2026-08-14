package org.zstack.header.cluster;

import org.zstack.header.message.NeedReplyMessage;

public class ReportHostCapacityMessage extends NeedReplyMessage {
    private long totalMemory;
    private long usedCpu;
    private long usedMemory;
    private String hostUuid;
    private int cpuNum;
    private int cpuSockets;
    private int cpuCoreNum;

    public int getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(int cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public int getCpuCoreNum() {
        return cpuCoreNum;
    }

    public void setCpuCoreNum(int cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
