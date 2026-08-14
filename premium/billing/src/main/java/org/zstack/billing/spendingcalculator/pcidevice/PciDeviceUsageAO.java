package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.billing.Usage;
import org.zstack.billing.UsageAO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by jie.wang on 2019/4/4.
 */

@MappedSuperclass
public class PciDeviceUsageAO  extends UsageAO implements Usage {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    @Column
    private String pciDeviceUuid;
    @Column
    private String vendorId;
    @Column
    private String deviceId;
    @Column
    private String subvendorId;
    @Column
    private String subdeviceId;
    @Column
    private String description;
    @Column
    private String vmUuid;
    @Column
    private String vmName;
    @Column
    private String status;
    @Column
    private String inventory;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Override
    public String getUsageId() {
        return pciDeviceUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
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

    public PciDeviceUsageAO(){
    }

    public PciDeviceUsageAO(PciDeviceUsageAO other) {
        this.id = other.id;
        this.pciDeviceUuid = other.pciDeviceUuid;
        this.vendorId = other.vendorId;
        this.deviceId = other.deviceId;
        this.subvendorId = other.subvendorId;
        this.subdeviceId = other.subdeviceId;
        this.description = other.description;
        this.vmUuid = other.vmUuid;
        this.vmName = other.vmName;
        this.status = other.status;
        this.inventory = other.inventory;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }
}
