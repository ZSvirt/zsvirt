package org.zstack.pciDevice.specification.pci;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.pciDevice.PciDeviceTO;
import org.zstack.pciDevice.PciDeviceType;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-03-05.
 */
@Entity
@Table
@BaseResource
public class PciDeviceSpecVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    // the unique id of the pci device vendor, see `lspci -mmnnv`
    @Column
    private String vendorId;

    @Column
    private String vendor;

    // see `lspci -mmnnv`
    @Column
    private String deviceId;

    @Column
    private String device;

    // see `lspci -mmnnv`
    @Column
    private String subvendorId;

    // see `lspci -mmnnv`
    @Column
    private String subdeviceId;

    // ram size of pci device
    @Column
    private String ramSize;

    // the number of total vf if pci device support sriov
    @Column
    private Integer maxPartNum;

    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceType type;

    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceSpecState state;

    @Column
    private boolean isVirtual;

    @Column
    private String romContent;

    @Column
    private String romVersion;

    @Column
    private String romMd5sum;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Transient
    private String accountUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSubvendorId() {
        return subvendorId;
    }

    public void setSubvendorId(String subvendorId) {
        this.subvendorId = subvendorId;
    }

    public String getSubdeviceId() {
        return subdeviceId;
    }

    public void setSubdeviceId(String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }

    public String getRamSize() {
        return ramSize;
    }

    public void setRamSize(String ramSize) {
        this.ramSize = ramSize;
    }

    public Integer getMaxPartNum() {
        return maxPartNum;
    }

    public void setMaxPartNum(Integer maxPartNum) {
        this.maxPartNum = maxPartNum;
    }

    public PciDeviceType getType() {
        return type;
    }

    public void setType(PciDeviceType type) {
        this.type = type;
    }

    public PciDeviceSpecState getState() {
        return state;
    }

    public void setState(PciDeviceSpecState state) {
        this.state = state;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    public String getRomContent() {
        return romContent;
    }

    public void setRomContent(String romContent) {
        this.romContent = romContent;
    }

    public String getRomVersion() {
        return romVersion;
    }

    public void setRomVersion(String romVersion) {
        this.romVersion = romVersion;
    }

    public String getRomMd5sum() {
        return romMd5sum;
    }

    public void setRomMd5sum(String romMd5sum) {
        this.romMd5sum = romMd5sum;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean matchPciDevice(PciDeviceTO that) {
        if (!vendorId.equals(that.getVendorId())) return false;
        if (!deviceId.equals(that.getDeviceId())) return false;
        if (StringUtils.isNotBlank(subvendorId) ? !subvendorId.equals(that.getSubvendorId()) : StringUtils.isNotBlank(that.getSubvendorId())) return false;
        if (StringUtils.isNotBlank(subdeviceId) ? !subdeviceId.equals(that.getSubdeviceId()) : StringUtils.isNotBlank(that.getSubdeviceId())) return false;
        return maxPartNum == that.getMaxPartNum();
    }

    @Override
    public String toString() {
        return "PciDeviceSpecVO{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", vendorId='" + vendorId + '\'' +
                ", vendor='" + vendor + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", device='" + device + '\'' +
                ", subvendorId='" + subvendorId + '\'' +
                ", subdeviceId='" + subdeviceId + '\'' +
                ", ramSize='" + ramSize + '\'' +
                ", maxPartNum=" + maxPartNum +
                ", type=" + type +
                ", state=" + state +
                ", isVirtual=" + isVirtual +
                ", romVersion='" + romVersion + '\'' +
                ", romMd5sum='" + romMd5sum + '\'' +
                ", createDate=" + createDate +
                ", lastOpDate=" + lastOpDate +
                '}';
    }
}
