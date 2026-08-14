package org.zstack.header.host;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 10:45
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class HostHwMonitorStatusVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus cpuStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus memoryStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus diskStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus nicStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus gpuStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus powerSupplyStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus fanStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus raidStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private HwMonitorStatus temperatureStatus;

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

    public HwMonitorStatus getNicStatus() {
        return nicStatus;
    }

    public void setNicStatus(HwMonitorStatus nicStatus) {
        this.nicStatus = nicStatus;
    }

    public HwMonitorStatus getGpuStatus() {
        return gpuStatus;
    }

    public void setGpuStatus(HwMonitorStatus gpuStatus) {
        this.gpuStatus = gpuStatus;
    }

    public HwMonitorStatus getPowerSupplyStatus() {
        return powerSupplyStatus;
    }

    public void setPowerSupplyStatus(HwMonitorStatus powerSupplyStatus) {
        this.powerSupplyStatus = powerSupplyStatus;
    }

    public HwMonitorStatus getFanStatus() {
        return fanStatus;
    }

    public void setFanStatus(HwMonitorStatus fanStatus) {
        this.fanStatus = fanStatus;
    }

    public HwMonitorStatus getRaidStatus() {
        return raidStatus;
    }

    public void setRaidStatus(HwMonitorStatus raidStatus) {
        this.raidStatus = raidStatus;
    }

    public HwMonitorStatus getTemperatureStatus() {
        return temperatureStatus;
    }

    public void setTemperatureStatus(HwMonitorStatus temperatureStatus) {
        this.temperatureStatus = temperatureStatus;
    }
}
