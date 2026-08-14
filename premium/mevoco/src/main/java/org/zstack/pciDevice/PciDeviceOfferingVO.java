package org.zstack.pciDevice;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by weiwang on 07/07/2017.
 */
@Entity
@Table
public class PciDeviceOfferingVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private PciDeviceOfferingType type;

    @Column
    private String vendorId;

    @Column
    private String deviceId;

    @Column
    private String subvendorId;

    @Column
    private String subdeviceId;

    @Column
    private String ramSize;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "pciDeviceOfferingUuid", insertable = false, updatable = false)
    @NoView
    private Set<PciDeviceOfferingInstanceOfferingRefVO> attachedInstanceOfferings = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "pciDeviceOfferingUuid", insertable = false, updatable = false)
    @NoView
    private Set<PciDevicePciDeviceOfferingRefVO> matchedPciDevices = new HashSet<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public boolean matchPciDevice(PciDeviceTO that) {
        if (!vendorId.equals(that.getVendorId())) return false;
        if (!deviceId.equals(that.getDeviceId())) return false;
        if (StringUtils.isNotBlank(subvendorId) ? !subvendorId.equals(that.getSubvendorId()) : StringUtils.isNotBlank(that.getSubvendorId())) return false;
        if (StringUtils.isNotBlank(subdeviceId) ? !subdeviceId.equals(that.getSubdeviceId()) : StringUtils.isNotBlank(that.getSubdeviceId())) return false;
        return (StringUtils.isNotBlank(ramSize) ? ramSize.equals(that.getRamSize()) : StringUtils.isBlank(that.getRamSize()));
    }

    public boolean matchPciDevice(PciDeviceVO that) {
        return this.matchPciDevice(PciDeviceTO.valueOf(that));
    }

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

    public PciDeviceOfferingType getType() {
        return type;
    }

    public void setType(PciDeviceOfferingType type) {
        this.type = type;
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

    public Set<PciDeviceOfferingInstanceOfferingRefVO> getAttachedInstanceOfferings() {
        return attachedInstanceOfferings;
    }

    public void setAttachedInstanceOfferings(Set<PciDeviceOfferingInstanceOfferingRefVO> attachedInstanceOfferings) {
        this.attachedInstanceOfferings = attachedInstanceOfferings;
    }

    public Set<PciDevicePciDeviceOfferingRefVO> getMatchedPciDevices() {
        return matchedPciDevices;
    }

    public void setMatchedPciDevices(Set<PciDevicePciDeviceOfferingRefVO> pciDevices) {
        this.matchedPciDevices = pciDevices;
    }
}
