package org.zstack.pciDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 10/07/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = PciDeviceOfferingVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "matchedPciDevice", inventoryClass = PciDevicePciDeviceOfferingRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "pciDeviceOfferingUuid"),
        @ExpandedQuery(expandedField = "attachedInstanceOfferingRef", inventoryClass = PciDeviceOfferingInstanceOfferingRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "pciDeviceOfferingUuid"),
})
public class PciDeviceOfferingInventory implements Serializable {
    private String uuid;

    private String name;

    private String description;

    private PciDeviceOfferingType type;

    private String vendorId;

    private String deviceId;

    private String subvendorId;

    private String subdeviceId;

    private String ramSize;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    @Queryable(mappingClass = PciDeviceOfferingInstanceOfferingRefInventory.class,
            joinColumn = @JoinColumn(name = "pciDeviceOfferingUuid"))
    private List<PciDeviceOfferingInstanceOfferingRefInventory> attachedInstanceOfferings;

    @Queryable(mappingClass = PciDevicePciDeviceOfferingRefInventory.class,
            joinColumn = @JoinColumn(name = "pciDeviceOfferingUuid"))
    private List<PciDevicePciDeviceOfferingRefInventory> matchedPciDevices;

    public PciDeviceOfferingInventory() {
    }

    public PciDeviceOfferingInventory(PciDeviceOfferingVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.type = vo.getType();
        this.vendorId = vo.getVendorId();
        this.deviceId = vo.getDeviceId();
        this.subvendorId = vo.getSubvendorId();
        this.subdeviceId = vo.getSubdeviceId();
        this.ramSize = vo.getRamSize();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static PciDeviceOfferingInventory valueOf(PciDeviceOfferingVO vo) {
        return new PciDeviceOfferingInventory(vo);
    }

    public static List<PciDeviceOfferingInventory> valueOf(Collection<PciDeviceOfferingVO> vos) {
        List<PciDeviceOfferingInventory> invs = new ArrayList<>();
        for (PciDeviceOfferingVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public List<PciDeviceOfferingInstanceOfferingRefInventory> getAttachedInstanceOfferings() {
        return attachedInstanceOfferings;
    }

    public void setAttachedInstanceOfferings(List<PciDeviceOfferingInstanceOfferingRefInventory> attachedInstanceOfferings) {
        this.attachedInstanceOfferings = attachedInstanceOfferings;
    }

    public List<PciDevicePciDeviceOfferingRefInventory> getMatchedPciDevices() {
        return matchedPciDevices;
    }

    public void setMatchedPciDevices(List<PciDevicePciDeviceOfferingRefInventory> matchedPciDevices) {
        this.matchedPciDevices = matchedPciDevices;
    }
}
