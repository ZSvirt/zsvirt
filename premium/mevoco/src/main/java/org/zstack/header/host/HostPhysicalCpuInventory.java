package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = HostPhysicalCpuVO.class)
public class HostPhysicalCpuInventory implements Serializable {
    private String uuid;
    private String hostUuid;
    private String serialNumber;
    private String socketDesignation;
    private String version;
    private String currentSpeed;
    private Integer coreCount;
    private Integer threadCount;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostPhysicalCpuInventory valueOf(HostPhysicalCpuVO vo) {
        HostPhysicalCpuInventory inventory = new HostPhysicalCpuInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setHostUuid(vo.getHostUuid());
        inventory.setSerialNumber(vo.getSerialNumber());
        inventory.setSocketDesignation(vo.getSocketDesignation());
        inventory.setVersion(vo.getVersion());
        inventory.setCurrentSpeed(vo.getCurrentSpeed());
        inventory.setCoreCount(vo.getCoreCount());
        inventory.setThreadCount(vo.getThreadCount());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<HostPhysicalCpuInventory> valueOf(List<HostPhysicalCpuVO> vos) {
        List<HostPhysicalCpuInventory> invs = new ArrayList<>(vos.size());
        for (HostPhysicalCpuVO vo : vos) {
            invs.add(HostPhysicalCpuInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSocketDesignation() {
        return socketDesignation;
    }

    public void setSocketDesignation(String socketDesignation) {
        this.socketDesignation = socketDesignation;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(String currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public Integer getCoreCount() {
        return coreCount;
    }

    public void setCoreCount(Integer coreCount) {
        this.coreCount = coreCount;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
