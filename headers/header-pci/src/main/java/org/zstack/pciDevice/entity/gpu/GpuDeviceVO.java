package org.zstack.pciDevice.entity.gpu;

import org.zstack.pciDevice.entity.PciDeviceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/7 13:10
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class GpuDeviceVO extends PciDeviceVO {
    @Column
    private String serialNumber;

    @Column
    private Long memory;

    @Column
    private Long power;

    @Column
    private boolean isDriverLoaded;

    public GpuDeviceVO() {

    }

    public GpuDeviceVO(PciDeviceVO vo) {
        super(vo);
    }

    public GpuDeviceVO update(PciDeviceVO vo) {
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setHostUuid(vo.getHostUuid());
        this.setParentUuid(vo.getParentUuid());
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setPciSpecUuid(vo.getPciSpecUuid());
        this.setType(vo.getType());
        this.setState(vo.getState());
        this.setStatus(vo.getStatus());
        this.setVirtStatus(vo.getVirtStatus());
        this.setPassThroughState(vo.getPassThroughState());
        this.setChooser(vo.getChooser());
        this.setVendorId(vo.getVendorId());
        this.setVendor(vo.getVendor());
        this.setDeviceId(vo.getDeviceId());
        this.setSubvendorId(vo.getSubvendorId());
        this.setSubdeviceId(vo.getSubdeviceId());
        this.setPciDeviceAddress(vo.getPciDeviceAddress());
        this.setIommuGroup(vo.getIommuGroup());
        this.setMetaData(vo.getMetaData());
        this.setDevice(vo.getDevice());
        return this;
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

    public boolean isDriverLoaded() {
        return isDriverLoaded;
    }

    public boolean getDriverLoaded() {
        return isDriverLoaded;
    }

    public void setDriverLoaded(boolean driverLoaded) {
        isDriverLoaded = driverLoaded;
    }
}
