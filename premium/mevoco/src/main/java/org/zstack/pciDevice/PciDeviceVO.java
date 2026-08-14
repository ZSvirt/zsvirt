package org.zstack.pciDevice;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;
import org.zstack.header.vo.Index;
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefVO;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by weiwang on 07/07/2017.
 */
@Entity
@Table
@BaseResource
@AutoDeleteTag
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid"),
        @SoftDeletionCascade(parent = VmInstanceEO.class, joinColumn = "vmInstanceUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PciDeviceVO.class, myField = "parentUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PciDeviceSpecVO.class, myField = "pciSpecUuid", targetField = "uuid"),
        }
)
public class PciDeviceVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Index
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    // if is physical pci device, then parentUuid is null
    // if is virtual pci device, then parentUuid is the uuid of releated physical pci device
    @Index
    @Column
    @ForeignKey(parentEntityClass = PciDeviceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String parentUuid;

    // the vm instance that this pci device attached to, null if not attached
    @Column
    @ForeignKey(parentEntityClass = VmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String vmInstanceUuid;

    // the specification of this pci device
    @Index
    @Column
    @ForeignKey(parentEntityClass = PciDeviceSpecVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String pciSpecUuid;

    // the type of this pci device, like GPU_Video_Controller/GPU_Audio_Controller/...
    @Index
    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceType type;

    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceState state;

    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceStatus status;

    // the virtualization status of this pci device
    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceVirtStatus virtStatus;

    @Column
    @Enumerated(EnumType.STRING)
    private PciDevicePassThroughState passThroughState;

    // spec or device
    @Column
    @Enumerated(EnumType.STRING)
    private PciDeviceChooser chooser;

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

    // see `lspci -mmnnv`
    @Column
    private String pciDeviceAddress;

    @Column
    private String iommuGroup;

    @Column
    private String metaData;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    // the specifications of mdev devices that this pci device can generate
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="pciDeviceUuid", insertable=false, updatable=false)
    @NoView
    private Set<PciDeviceMdevSpecRefVO> mdevSpecRefs = new HashSet<>();

    @Transient
    private String accountUuid;

    @Transient
    private Map<String, String> addonInfo = new HashMap<>();

    public PciDeviceVO() {
    }

    public PciDeviceVO(PciDeviceVO vo) {
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
        this.passThroughState = vo.getPassThroughState();
        this.chooser = vo.getChooser();
        this.vendorId = vo.getVendorId();
        this.vendor = vo.getVendor();
        this.deviceId = vo.getDeviceId();
        this.device = vo.getDevice();
        this.subdeviceId = vo.getSubdeviceId();
        this.subvendorId = vo.getSubvendorId();
        this.pciDeviceAddress = vo.getPciDeviceAddress();
        this.iommuGroup = vo.getIommuGroup();
        this.metaData = vo.getMetaData();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.accountUuid = vo.getAccountUuid();
        this.addonInfo = vo.getAddonInfo();

    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public PciDeviceAddress getThePciDeviceAddress() {
        return new PciDeviceAddress(pciDeviceAddress);
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

    public String getMetaData() {
        return metaData;
    }

    public PciDeviceMetaData getPciDeviceMetaData() {
        return new PciDeviceMetaData(metaData);
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public void setPciDeviceMetaData(PciDeviceMetaData pciDeviceMetaData) {
        this.setMetaData(pciDeviceMetaData.toString());
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

    public Set<PciDeviceMdevSpecRefVO> getMdevSpecRefs() {
        return mdevSpecRefs;
    }

    public void setMdevSpecRefs(Set<PciDeviceMdevSpecRefVO> mdevSpecRefs) {
        this.mdevSpecRefs = mdevSpecRefs;
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

    public Map<String, String> getAddonInfo() {
        return addonInfo;
    }

    public void setAddonInfo(Map<String, String> addonInfo) {
        this.addonInfo = addonInfo;
    }

    @Override
    public String toString() {
        return "PciDeviceVO{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", hostUuid='" + hostUuid + '\'' +
                ", parentUuid='" + parentUuid + '\'' +
                ", vmInstanceUuid='" + vmInstanceUuid + '\'' +
                ", pciSpecUuid='" + pciSpecUuid + '\'' +
                ", type=" + type +
                ", state=" + state +
                ", status=" + status +
                ", virtStatus=" + virtStatus +
                ", passThroughState=" + passThroughState +
                ", chooser=" + chooser +
                ", vendorId='" + vendorId + '\'' +
                ", vendor='" + vendor + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", device='" + device + '\'' +
                ", subvendorId='" + subvendorId + '\'' +
                ", subdeviceId='" + subdeviceId + '\'' +
                ", pciDeviceAddress='" + pciDeviceAddress + '\'' +
                ", iommuGroup='" + iommuGroup + '\'' +
                ", metaData='" + metaData + '\'' +
                ", createDate=" + createDate +
                ", lastOpDate=" + lastOpDate +
                ", mdevSpecRefs=" + mdevSpecRefs +
                ", accountUuid='" + accountUuid + '\'' +
                '}';
    }
}
