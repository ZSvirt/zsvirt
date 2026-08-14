package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * author:kaicai.hu
 * Date:2021/8/6
 */
@PythonClassInventory
@Inventory(mappingVOClass = HostPhysicalMemoryVO.class)
public class HostPhysicalMemoryInventory implements Serializable {
    private String uuid;
    private String hostUuid;
    private String manufacturer;
    private String size;
    private String speed;
    private String clockSpeed;
    private String locator;
    private String serialNumber;
    private String rank;
    private String voltage;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostPhysicalMemoryInventory valueOf(HostPhysicalMemoryVO vo) {
        HostPhysicalMemoryInventory inventory = new HostPhysicalMemoryInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setHostUuid(vo.getHostUuid());
        inventory.setManufacturer(vo.getManufacturer());
        inventory.setSize(vo.getSize());
        inventory.setSpeed(vo.getSpeed());
        inventory.setClockSpeed(vo.getClockSpeed());
        inventory.setLocator(vo.getLocator());
        inventory.setSerialNumber(vo.getSerialNumber());
        inventory.setRank(vo.getRank());
        inventory.setVoltage(vo.getVoltage());
        inventory.setType(vo.getType());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<HostPhysicalMemoryInventory> valueOf(List<HostPhysicalMemoryVO> vos) {
        List<HostPhysicalMemoryInventory> invs = new ArrayList<>(vos.size());
        for (HostPhysicalMemoryVO vo : vos) {
            invs.add(HostPhysicalMemoryInventory.valueOf(vo));
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getVoltage() {
        return voltage;
    }

    public void setVoltage(String voltage) {
        this.voltage = voltage;
    }

    public String getClockSpeed() {
        return clockSpeed;
    }

    public void setClockSpeed(String clockSpeed) {
        this.clockSpeed = clockSpeed;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
