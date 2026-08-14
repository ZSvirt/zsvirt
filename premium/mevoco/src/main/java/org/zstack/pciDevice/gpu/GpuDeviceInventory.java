package org.zstack.pciDevice.gpu;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.pciDevice.PciDeviceInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/7 13:21
 */
@PythonClassInventory
@Inventory(mappingVOClass = GpuDeviceVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = GpuDeviceInventory.class, type = GpuConstant.GPU_DEVICE_TYPE)})
public class GpuDeviceInventory extends PciDeviceInventory {
    private String serialNumber;

    private Long memory;

    private Long power;
    private Boolean isDriverLoaded;

    public GpuDeviceInventory() {
    }

    public GpuDeviceInventory(GpuDeviceVO vo) {
        super(vo);
        this.serialNumber = vo.getSerialNumber();
        this.memory = vo.getMemory();
        this.power = vo.getPower();
        this.isDriverLoaded = vo.getDriverLoaded();
    }


    public static GpuDeviceInventory valueOf(GpuDeviceVO vo) {
        return new GpuDeviceInventory(vo);
    }

    public static List<GpuDeviceInventory> valueOf1(Collection<GpuDeviceVO> vos) {
        List<GpuDeviceInventory> invs = new ArrayList<>();
        for (GpuDeviceVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }


    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public Long getPower() {
        return power;
    }

    public void setPower(Long power) {
        this.power = power;
    }

    public Boolean getDriverLoaded() {
        return isDriverLoaded;
    }

    public void setDriverLoaded(Boolean driverLoaded) {
        isDriverLoaded = driverLoaded;
    }
}
