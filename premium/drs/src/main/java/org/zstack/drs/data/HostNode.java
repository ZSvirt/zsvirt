package org.zstack.drs.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2019/12/6.
 */
public class HostNode implements Serializable {
    private String uuid;
    private List<VmNode> vmList = new ArrayList<>();

    private float usedCPUPercent; // [0, 100]
    private float freeCPUPercent; // [0, 100]
    private Float maxSize2MoveCPUPercent; // Space that needs to be moved or can be moved in，[0, 100]

    private float usedMemoryPercent; // [0, 100]
    private float freeMemoryPercent; // [0, 100]
    private long freePhysicalMemoryBit;
    private long totalMemoryBit;
    private Long maxSize2MoveMemoryBit;

    private long hostCapacityAvailableCpuNum;
    private long hostCapacityAvailableMemoryBit;

    public HostNode(String hostUuid, float usedCPUPercent, long usedPhysicalMemoryBit, long hostCapacityAvailableCpuNum, long hostCapacityAvailableMemoryBit, long totalMemoryBit) {
        this.uuid = hostUuid;
        this.usedCPUPercent = usedCPUPercent;
        this.freeCPUPercent = 100 - usedCPUPercent;
        this.freePhysicalMemoryBit = totalMemoryBit - usedPhysicalMemoryBit;
        this.freeMemoryPercent = (int) ((1f * freePhysicalMemoryBit / totalMemoryBit) * 100);
        this.usedMemoryPercent = 100 - freeMemoryPercent;

        this.hostCapacityAvailableCpuNum = hostCapacityAvailableCpuNum;
        this.hostCapacityAvailableMemoryBit = hostCapacityAvailableMemoryBit;
        this.totalMemoryBit = totalMemoryBit;
    }

    private void updateMemoryPercent() {
        this.freeMemoryPercent = (int) ((1f * freePhysicalMemoryBit / totalMemoryBit) * 100);
        this.usedMemoryPercent = 100 - freeMemoryPercent;
    }

    public float getUsedCPUPercent() {
        return usedCPUPercent;
    }

    public void setUsedCPUPercent(float usedCPUPercent) {
        this.usedCPUPercent = usedCPUPercent;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public float getFreeCPUPercent() {
        return freeCPUPercent;
    }

    public void setFreeCPUPercent(float freeCPUPercent) {
        this.freeCPUPercent = freeCPUPercent;
    }

    public Float getMaxSize2MoveCPUPercent() {
        return maxSize2MoveCPUPercent;
    }

    public void setMaxSize2MoveCPUPercent(Float maxSize2MoveCPUPercent) {
        this.maxSize2MoveCPUPercent = maxSize2MoveCPUPercent;
    }

    public long getHostCapacityAvailableCpuNum() {
        return hostCapacityAvailableCpuNum;
    }

    public void setHostCapacityAvailableCpuNum(long hostCapacityAvailableCpuNum) {
        this.hostCapacityAvailableCpuNum = hostCapacityAvailableCpuNum;
    }

    public long getHostCapacityAvailableMemoryBit() {
        return hostCapacityAvailableMemoryBit;
    }

    public void setHostCapacityAvailableMemoryBit(long hostCapacityAvailableMemoryBit) {
        this.hostCapacityAvailableMemoryBit = hostCapacityAvailableMemoryBit;
    }

    public long getFreePhysicalMemoryBit() {
        return freePhysicalMemoryBit;
    }

    public void setFreePhysicalMemoryBit(long freePhysicalMemoryBit) {
        this.freePhysicalMemoryBit = freePhysicalMemoryBit;
        updateMemoryPercent();
    }

    public long getTotalMemoryBit() {
        return totalMemoryBit;
    }

    private void setTotalMemoryBit(long totalMemoryBit) {
        this.totalMemoryBit = totalMemoryBit;
    }

    public Long getMaxSize2MoveMemoryBit() {
        return maxSize2MoveMemoryBit;
    }

    public void setMaxSize2MoveMemoryBit(Long maxSize2MoveMemoryBit) {
        this.maxSize2MoveMemoryBit = maxSize2MoveMemoryBit;
    }

    public List<VmNode> getVmList() {
        return vmList;
    }

    public void setVmList(List<VmNode> vmList) {
        this.vmList = vmList;
    }

    public float getUsedMemoryPercent() {
        return usedMemoryPercent;
    }

    private void setUsedMemoryPercent(float usedMemoryPercent) {
        this.usedMemoryPercent = usedMemoryPercent;
    }

    public float getFreeMemoryPercent() {
        return freeMemoryPercent;
    }

    public void setFreeMemoryPercent(float freeMemoryPercent) {
        this.freeMemoryPercent = freeMemoryPercent;
    }
}
