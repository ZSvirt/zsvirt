package org.zstack.header.allocator;

import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = HostCapacityVO.class)
public class HostCapacityInventory {
    private String uuid;
    private Long totalMemory;
    private Long totalCpu;
    private Integer cpuNum;
    private Integer cpuSockets;
    private Integer cpuCoreNum;
    private Long availableMemory;
    private Long availableCpu;
    private Long totalPhysicalMemory;
    private Long availablePhysicalMemory;

    public static HostCapacityInventory valueOf(HostCapacityVO vo) {
        HostCapacityInventory inv = new HostCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalMemory(vo.getTotalMemory());
        inv.setAvailableMemory(vo.getAvailableMemory());
        inv.setTotalCpu(vo.getTotalCpu());
        inv.setAvailableCpu(vo.getAvailableCpu());
        inv.setAvailablePhysicalMemory(vo.getAvailablePhysicalMemory());
        inv.setTotalPhysicalMemory(vo.getTotalPhysicalMemory());
        inv.setCpuNum(vo.getCpuNum());
        inv.setCpuSockets(vo.getCpuSockets());
        inv.setCpuCoreNum(vo.getCpuCoreNum());
        return inv;
    }

    public static List<HostCapacityInventory> valueOf(Collection<HostCapacityVO> vos) {
        return CollectionUtils.transform(vos, HostCapacityInventory::valueOf);
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public Integer getCpuCoreNum() {
        return cpuCoreNum;
    }

    public void setCpuCoreNum(Integer cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getTotalPhysicalMemory() {
        return totalPhysicalMemory;
    }

    public void setTotalPhysicalMemory(Long totalPhysicalMemory) {
        this.totalPhysicalMemory = totalPhysicalMemory;
    }

    public Long getAvailablePhysicalMemory() {
        return availablePhysicalMemory;
    }

    public void setAvailablePhysicalMemory(Long availablePhysicalMemory) {
        this.availablePhysicalMemory = availablePhysicalMemory;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getTotalCpu() {
        return totalCpu;
    }

    public void setTotalCpu(long totalCpu) {
        this.totalCpu = totalCpu;
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public long getAvailableCpu() {
        return availableCpu;
    }

    public void setAvailableCpu(long availableCpu) {
        this.availableCpu = availableCpu;
    }
}
