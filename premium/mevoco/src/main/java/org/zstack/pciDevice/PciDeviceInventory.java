package org.zstack.pciDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefInventory;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecInventory;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

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
@Inventory(mappingVOClass = PciDeviceVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "parent", inventoryClass = PciDeviceInventory.class,
                foreignKey = "parentUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pciDeviceSpec", inventoryClass = PciDeviceSpecInventory.class,
                foreignKey = "pciSpecUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "mdevSpecRefs", inventoryClass = PciDeviceMdevSpecRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "pciDeviceUuid"),
        @ExpandedQuery(expandedField = "matchedPciDeviceOffering", inventoryClass = PciDevicePciDeviceOfferingRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "pciDeviceUuid"),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "mdevSpec", expandedField = "mdevSpecRefs.mdevSpec")
})
public class PciDeviceInventory implements Serializable {
    private String uuid;

    private String name;

    private String description;

    private String hostUuid;

    private String parentUuid;

    private String vmInstanceUuid;

    private String pciSpecUuid;

    private PciDeviceType type;

    private PciDeviceState state;

    private PciDeviceStatus status;

    private PciDeviceVirtStatus virtStatus;

    private PciDevicePassThroughState passThroughState;

    private PciDeviceChooser chooser;

    private String vendorId;

    private String vendor;
    private String deviceId;

    private String device;

    private String subvendorId;

    private String subdeviceId;

    private String pciDeviceAddress;

    private String iommuGroup;

    private PciDeviceMetaData metaData;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    @Queryable(mappingClass = PciDevicePciDeviceOfferingRefInventory.class,
            joinColumn = @JoinColumn(name = "pciDeviceUuid"))
    private List<PciDevicePciDeviceOfferingRefInventory> matchedPciDeviceOfferingRef;

    @Queryable(mappingClass = PciDeviceMdevSpecRefInventory.class,
            joinColumn = @JoinColumn(name="pciSpecUuid", referencedColumnName = "mdevSpecUuid"))
    private List<PciDeviceMdevSpecRefInventory> mdevSpecRefs;

    public PciDeviceInventory() {
    }

    public PciDeviceInventory(PciDeviceVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.hostUuid = vo.getHostUuid();
        this.parentUuid = vo.getParentUuid();
        this.vmInstanceUuid = vo.getVmInstanceUuid();
        this.pciSpecUuid = vo.getPciSpecUuid();
        this.type = vo.getType();
        this.state = vo.getState();
        this.status = vo.getStatus();
        this.virtStatus = vo.getVirtStatus();
        this.chooser = vo.getChooser();
        this.passThroughState = vo.getPassThroughState();
        this.vendorId = vo.getVendorId();
        this.vendor = vo.getVendor();
        this.deviceId = vo.getDeviceId();
        this.device = vo.getDevice();
        this.subvendorId = vo.getSubvendorId();
        this.subdeviceId = vo.getSubdeviceId();
        this.pciDeviceAddress = vo.getPciDeviceAddress();
        this.iommuGroup = vo.getIommuGroup();
        this.metaData = vo.getPciDeviceMetaData();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.mdevSpecRefs = PciDeviceMdevSpecRefInventory.valueOf(vo.getMdevSpecRefs());
    }

    public static PciDeviceInventory valueOf(PciDeviceVO vo) {
        return new PciDeviceInventory(vo);
    }

    public static List<PciDeviceInventory> valueOf(Collection<PciDeviceVO> vos) {
        List<PciDeviceInventory> invs = new ArrayList<>();
        for (PciDeviceVO vo : vos) {
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPciSpecUuid() {
        return pciSpecUuid;
    }

    public void setPciSpecUuid(String pciSpecUuid) {
        this.pciSpecUuid = pciSpecUuid;
    }

    public PciDeviceState getState() {
        return state;
    }

    public void setState(PciDeviceState state) {
        this.state = state;
    }

    public PciDeviceStatus getStatus() {
        return status;
    }

    public void setStatus(PciDeviceStatus status) {
        this.status = status;
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

    public String getPciDeviceAddress() {
        return pciDeviceAddress;
    }

    public void setPciDeviceAddress(String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }

    public String getIommuGroup() {
        return iommuGroup;
    }

    public void setIommuGroup(String iommuGroup) {
        this.iommuGroup = iommuGroup;
    }

    public PciDeviceType getType() {
        return type;
    }

    public void setType(PciDeviceType type) {
        this.type = type;
    }

    public PciDeviceVirtStatus getVirtStatus() {
        return virtStatus;
    }

    public void setVirtStatus(PciDeviceVirtStatus virtStatus) {
        this.virtStatus = virtStatus;
    }

    public PciDevicePassThroughState getPassThroughState() {
        return passThroughState;
    }

    public void setPassThroughState(PciDevicePassThroughState passThroughState) {
        this.passThroughState = passThroughState;
    }

    public PciDeviceChooser getChooser() {
        return chooser;
    }

    public void setChooser(PciDeviceChooser chooser) {
        this.chooser = chooser;
    }

    public PciDeviceMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(PciDeviceMetaData metaData) {
        this.metaData = metaData;
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

    public List<PciDeviceMdevSpecRefInventory> getMdevSpecRefs() {
        return mdevSpecRefs;
    }

    public void setMdevSpecRefs(List<PciDeviceMdevSpecRefInventory> mdevSpecRefs) {
        this.mdevSpecRefs = mdevSpecRefs;
    }

    public List<PciDevicePciDeviceOfferingRefInventory> getMatchedPciDeviceOfferingRef() {
        return matchedPciDeviceOfferingRef;
    }

    public void setMatchedPciDeviceOfferingRef(List<PciDevicePciDeviceOfferingRefInventory> matchedPciDeviceOfferingRef) {
        this.matchedPciDeviceOfferingRef = matchedPciDeviceOfferingRef;
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
}
