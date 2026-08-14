package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by shixin.ruan on 2018/05/07.
 */
@Inventory(mappingVOClass = PciDeviceUsageVO.class)
public class PciDeviceUsageInventory {
    private Long id;
    private String accountUuid;
    private Long dateInLong;
    private String pciDeviceUuid;
    private String vendorId;
    private String deviceId;
    private String subvendorId;
    private String subdeviceId;
    private String description;
    private String vmUuid;
    private String vmName;
    private String status;
    private String inventory;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PciDeviceUsageInventory valueOf(PciDeviceUsageVO vo) {
        PciDeviceUsageInventory inv = new PciDeviceUsageInventory();
        inv.setId(vo.getId());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setDateInLong(vo.getDateInLong());
        inv.setPciDeviceUuid(vo.getPciDeviceUuid());
        inv.setVendorId(vo.getVendorId());
        inv.setDeviceId(vo.getDeviceId());
        inv.setSubvendorId(vo.getSubvendorId());
        inv.setSubdeviceId(vo.getSubdeviceId());
        inv.setVmUuid(vo.getVmUuid());
        inv.setVmName(vo.getVmName());
        inv.setStatus(vo.getStatus());
        inv.setInventory(vo.getInventory());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setDescription(vo.getDescription());
        return inv;
    }

    public static List<PciDeviceUsageInventory> valueOf(Collection<PciDeviceUsageVO> vos) {
        return vos.stream().map(PciDeviceUsageInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(long dateInLong) {
        this.dateInLong = dateInLong;
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

    public void setDateInLong(Long dateInLong) {
        this.dateInLong = dateInLong;
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }
}
