package org.zstack.pciDevice.gpu;

import org.zstack.pciDevice.PciDeviceVO;

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
