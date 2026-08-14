package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 11:00
 */
@PythonClassInventory
@Inventory(mappingVOClass = HostHwMonitorStatusVO.class, collectionValueOfMethod = "valueOf1")
public class HostHwMonitorStatusInventory implements Serializable {
    private String uuid;
    private HwMonitorStatus cpuStatus;
    private HwMonitorStatus memoryStatus;
    private HwMonitorStatus diskStatus;
    private HwMonitorStatus fanStatus;
    private HwMonitorStatus powerSupplyStatus;
    private HwMonitorStatus raidStatus;
    private HwMonitorStatus networkStatus;
    private HwMonitorStatus gpuStatus;
    private HwMonitorStatus temperatureStatus;

    protected HostHwMonitorStatusInventory(HostHwMonitorStatusVO vo) {
        this.setUuid(vo.getUuid());
        this.setCpuStatus(vo.getCpuStatus());
        this.setMemoryStatus(vo.getMemoryStatus());
        this.setDiskStatus(vo.getDiskStatus());
        this.setFanStatus(vo.getFanStatus());
        this.setPowerSupplyStatus(vo.getPowerSupplyStatus());
        this.setRaidStatus(vo.getRaidStatus());
        this.setNetworkStatus(vo.getNicStatus());
        this.setGpuStatus(vo.getGpuStatus());
        this.setTemperatureStatus(vo.getTemperatureStatus());
    }

    public static HostHwMonitorStatusInventory valueOf(HostHwMonitorStatusVO vo) {
        return new HostHwMonitorStatusInventory(vo);
    }

    public static List<HostHwMonitorStatusInventory> valueOf1(Collection<HostHwMonitorStatusVO> vos) {
        List<HostHwMonitorStatusInventory> invs = new ArrayList<HostHwMonitorStatusInventory>(vos.size());
        for (HostHwMonitorStatusVO vo : vos) {
            invs.add(HostHwMonitorStatusInventory.valueOf(vo));
        }
        return invs;
    }

    public HostHwMonitorStatusInventory() {
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public HwMonitorStatus getCpuStatus() {
        return cpuStatus;
    }

    public void setCpuStatus(HwMonitorStatus cpuStatus) {
        this.cpuStatus = cpuStatus;
    }

    public HwMonitorStatus getMemoryStatus() {
        return memoryStatus;
    }

    public void setMemoryStatus(HwMonitorStatus memoryStatus) {
        this.memoryStatus = memoryStatus;
    }

    public HwMonitorStatus getDiskStatus() {
        return diskStatus;
    }

    public void setDiskStatus(HwMonitorStatus diskStatus) {
        this.diskStatus = diskStatus;
    }

    public HwMonitorStatus getFanStatus() {
        return fanStatus;
    }

    public void setFanStatus(HwMonitorStatus fanStatus) {
        this.fanStatus = fanStatus;
    }

    public HwMonitorStatus getPowerSupplyStatus() {
        return powerSupplyStatus;
    }

    public void setPowerSupplyStatus(HwMonitorStatus powerSupplyStatus) {
        this.powerSupplyStatus = powerSupplyStatus;
    }

    public HwMonitorStatus getRaidStatus() {
        return raidStatus;
    }

    public void setRaidStatus(HwMonitorStatus raidStatus) {
        this.raidStatus = raidStatus;
    }

    public HwMonitorStatus getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(HwMonitorStatus networkStatus) {
        this.networkStatus = networkStatus;
    }

    public HwMonitorStatus getGpuStatus() {
        return gpuStatus;
    }

    public void setGpuStatus(HwMonitorStatus gpuStatus) {
        this.gpuStatus = gpuStatus;
    }

    public HwMonitorStatus getTemperatureStatus() {
        return temperatureStatus;
    }

    public void setTemperatureStatus(HwMonitorStatus temperatureStatus) {
        this.temperatureStatus = temperatureStatus;
    }
}
