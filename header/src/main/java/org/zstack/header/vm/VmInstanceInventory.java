package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @inventory inventory for vm instance
 * @example {
 * "inventory": {
 * "uuid": "94d991c631674b16be65bfdf28b9e84a",
 * "name": "TestVm",
 * "description": "Test",
 * "zoneUuid": "acadddc85a604db4b1b7358605cd6015",
 * "clusterUuid": "f6cd5db05a0d49d8b12721e0bf721b4c",
 * "imageUuid": "061141410a0449b6919b50e90d68b7cd",
 * "hostUuid": "908131845d284d7f821a74362fff3d19",
 * "lastHostUuid": "908131845d284d7f821a74362fff3d19",
 * "instanceOfferingUuid": "91cb47f1416748afa7e0d34f4d0731ef",
 * "rootVolumeUuid": "19aa7ec504a247d89b511b322ffa483c",
 * "type": "UserVm",
 * "hypervisorType": "KVM",
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM",
 * "state": "Running",
 * "internalId": 1,
 * "vmNics": [
 * {
 * "uuid": "6b58e6b2ba174ef4bce8a549de9560e8",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "4ecc80a2d1d93d48b32680827542ddbb",
 * "l3NetworkUuid": "55f85b8fa9a647f1be251787c66550ee",
 * "ip": "10.12.140.148",
 * "mac": "fa:f0:08:8c:20:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM"
 * },
 * {
 * "uuid": "889cfcab8c08409296c649611a4df50c",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "8877537e11783ee0bfe8af0fcf7a6388",
 * "l3NetworkUuid": "c6134efd3af94db7b2928ddc5deba540",
 * "ip": "10.4.224.72",
 * "mac": "fa:e3:87:b1:71:01",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "internalName": "vnic1.1",
 * "deviceId": 1,
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM"
 * },
 * {
 * "uuid": "cba0da7a12d44b2e878dd5803d078337",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "f90d01a098303956823ced02438ae3ab",
 * "l3NetworkUuid": "c7e9e14f2af742c29c3e25d58f16a45f",
 * "ip": "10.29.42.155",
 * "mac": "fa:2d:31:08:da:02",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.3.1",
 * "internalName": "vnic1.2",
 * "deviceId": 2,
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM"
 * },
 * {
 * "uuid": "f31e38309e2047beac588e111fa2051f",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "usedIpUuid": "4ce077085c7e355d988450f11ce767b7",
 * "l3NetworkUuid": "e438b93332ba40dcbb5d553c749a43ca",
 * "ip": "10.20.206.157",
 * "mac": "fa:a3:04:b2:6c:00",
 * "netmask": "255.0.0.0",
 * "gateway": "10.20.4.1",
 * "internalName": "vnic1.0",
 * "deviceId": 0,
 * "createDate": "Apr 30, 2014 6:11:48 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:48 PM"
 * }
 * ],
 * "allVolumes": [
 * {
 * "uuid": "19aa7ec504a247d89b511b322ffa483c",
 * "name": "ROOT-for-TestVm",
 * "description": "Root volume for VM[uuid:94d991c631674b16be65bfdf28b9e84a]",
 * "primaryStorageUuid": "24931b95b45e41fb8e41a640302d4c00",
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "rootImageUuid": "061141410a0449b6919b50e90d68b7cd",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-24931b95b45e41fb8e41a640302d4c00/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-19aa7ec504a247d89b511b322ffa483c/19aa7ec504a247d89b511b322ffa483c.qcow2",
 * "type": "Root",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 0,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 6:11:47 PM",
 * "lastOpDate": "Apr 30, 2014 6:11:47 PM",
 * "backupStorageRefs": []
 * }
 * ]
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = VmInstanceVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "image", inventoryClass = ImageInventory.class,
                foreignKey = "imageUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "instanceOffering", inventoryClass = InstanceOfferingInventory.class,
                foreignKey = "instanceOfferingUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "rootVolume", inventoryClass = VolumeInventory.class,
                foreignKey = "rootVolumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmNics", inventoryClass = VmNicInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmInstanceUuid"),
        @ExpandedQuery(expandedField = "allVolumes", inventoryClass = VolumeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmInstanceUuid"),
        @ExpandedQuery(expandedField = "vmCdRoms", inventoryClass = VmCdRomInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmInstanceUuid"),
})
public class VmInstanceInventory implements Serializable, Cloneable {
    /**
     * @desc vm uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc uuid of zone this vm is in. See :ref:`ZoneInventory`
     */
    private String zoneUuid;
    /**
     * @desc uuid of cluster this vm is in. See :ref:`ClusterInventory`
     */
    private String clusterUuid;
    /**
     * @desc uuid of image this vm was created from. See :ref:`ImageInventory`
     */
    private String imageUuid;
    /**
     * @desc uuid of host the vm is on. See :ref:`HostInventory`
     * @nullable .. note:: this field is null when vm is stopped
     */
    private String hostUuid;
    /**
     * @desc uuid of host the vm was running on last time
     * @nullable .. note:: this field is null when vm has not been stopped yet. Once vm gets stopped, this
     * field is filled with host uuid it's running on previously
     */
    private String lastHostUuid;
    /**
     * @desc uuid of instance offering the vm was created from. See :ref:`InstanceOfferingInventory`
     */
    private String instanceOfferingUuid;
    /**
     * @desc uuid of vm's root volume. See :ref:`VolumeInventory`
     */
    private String rootVolumeUuid;

    private String platform;

    private String architecture;

    private String defaultL3NetworkUuid;
    /**
     * @desc - UserVm: normal vm
     * - ApplianceVm: special vm created by zstack to provide service for the cloud, for example, virtual router
     * provides network services like DHCP/SNAT. User except admin should not see this type of vm
     * @choices - UserVm
     * - ApplianceVm
     */
    @TypeField
    private String type;
    /**
     * @desc hypervisor type of vm. See hypervisorType of :ref:`ClusterInventory`
     */
    private String hypervisorType;

    private Long memorySize;

    private Long reservedMemorySize;

    private Integer cpuNum;

    private Long cpuSpeed;

    private String allocatorStrategy;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc whether VM sensitive system tags are encrypted
     */
    private Boolean vmEncryption;
    /**
     * @desc whether the vm itself, any attached volume, or its TPM has encrypted resource
     */
    private Boolean hasEncryptionResource;
    /**
     * @desc latest key update time of attached encrypted volumes or TPM; falls back to vm create date when only vm itself is encrypted
     * @nullable
     */
    private Timestamp keyLastOpDate;
    /**
     * @desc - Created: the vm is just created in database, having not been started
     * - Starting: the vm is starting, having not run on host
     * - Running: the vm is running on host
     * - Stopping: the vm is stopping, having not stopped on host
     * - Stopped: the vm is stopped on host
     * - Rebooting: the vm is in middle way of rebooting before running on host again
     * - Destroying: the vm is destroying, having not destroyed on host
     * - Migrating: the vm is migrating to another host
     * - Unknown: zstack cannot track vm state, for example, lost connection to hypervisor agent
     * @choices - Created
     * - Starting
     * - Running
     * - Stopping
     * - Rebooting
     * - Destroying
     * - Migrating
     * - Unknown
     */
    private String state;
    /**
     * @ignore
     */
    @APINoSee
    private Long internalId;
    /**
     * @desc a list of nics the vm has. See :ref:`VmNicInventory`
     */
    private List<VmNicInventory> vmNics;
    /**
     * @desc a list of volumes the vm has, including all types volume.
     * See :{@link VolumeInventory}
     * See :{@link VolumeType}
     */
    private List<VolumeInventory> allVolumes;

    private List<VmCdRomInventory> vmCdRoms;

    private String guestOsType;

    protected VmInstanceInventory(VmInstanceVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setZoneUuid(vo.getZoneUuid());
        this.setClusterUuid(vo.getClusterUuid());
        this.setImageUuid(vo.getImageUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setLastHostUuid(vo.getLastHostUuid());
        this.setInstanceOfferingUuid(vo.getInstanceOfferingUuid());
        this.setType(vo.getType());
        this.setHypervisorType(vo.getHypervisorType());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setState(vo.getState().toString());
        this.setRootVolumeUuid(vo.getRootVolumeUuid());
        this.setAllVolumes(VolumeInventory.valueOf(vo.getAllVolumes()));
        this.setVmNics(VmNicInventory.valueOf(vo.getVmNics()));
        this.setVmCdRoms(VmCdRomInventory.valueOf(vo.getVmCdRoms()));
        this.setInternalId(vo.getInternalId());
        this.setDefaultL3NetworkUuid(vo.getDefaultL3NetworkUuid());
        this.setCpuNum(vo.getCpuNum());
        this.setCpuSpeed(vo.getCpuSpeed());
        this.setMemorySize(vo.getMemorySize());
        this.setReservedMemorySize(vo.getReservedMemorySize());
        this.setAllocatorStrategy(vo.getAllocatorStrategy());
        this.setPlatform(vo.getPlatform());
        this.setArchitecture(vo.getArchitecture());
        this.setGuestOsType(vo.getGuestOsType());
        this.setVmEncryption(vo.isVmEncryption());
    }

    public static VmInstanceInventory valueOf(VmInstanceVO vo) {
        return new VmInstanceInventory(vo);
    }

    public static List<VmInstanceInventory> valueOf(Collection<VmInstanceVO> vos) {
        List<VmInstanceInventory> invs = new ArrayList<VmInstanceInventory>(vos.size());
        for (VmInstanceVO vo : vos) {
            invs.add(VmInstanceInventory.valueOf(vo));
        }
        return invs;
    }

    public VmInstanceInventory() {
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public VmNicInventory findNic(final String l3Uuid) {
        return CollectionUtils.findOneOrNull(vmNics, arg -> VmNicHelper.isL3AttachedToVmNic(arg, l3Uuid));
    }

    public static VmInstanceInventory copyFrom(VmInstanceInventory origin) {
        try {
            return (VmInstanceInventory) origin.clone();
        } catch (CloneNotSupportedException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public VmInstanceInventory(VmInstanceInventory origin) {
        VmInstanceInventory inv;
        try {
            inv = (VmInstanceInventory) origin.clone();
        } catch (CloneNotSupportedException e) {
            throw new CloudRuntimeException(e);
        }

        this.setClusterUuid(inv.getClusterUuid());
        this.setCreateDate(inv.getCreateDate());
        this.setDescription(inv.getDescription());
        this.setHostUuid(inv.getHostUuid());
        this.setHypervisorType(inv.getHypervisorType());
        this.setImageUuid(inv.getImageUuid());
        this.setInstanceOfferingUuid(inv.getInstanceOfferingUuid());
        this.setLastHostUuid(inv.getLastHostUuid());
        this.setLastOpDate(inv.getLastOpDate());
        this.setName(inv.getName());
        this.setRootVolumeUuid(inv.getRootVolumeUuid());
        this.setState(inv.getState());
        this.setType(inv.getType());
        this.setUuid(inv.getUuid());
        this.setZoneUuid(inv.getZoneUuid());
        this.setAllVolumes(inv.getAllVolumes());
        this.setVmNics(inv.getVmNics());
        this.setInternalId(inv.getInternalId());
        this.setCpuNum(inv.getCpuNum());
        this.setCpuSpeed(inv.getCpuSpeed());
        this.setMemorySize(inv.getMemorySize());
        this.setReservedMemorySize(inv.getReservedMemorySize());
        this.setAllocatorStrategy(inv.getAllocatorStrategy());
        this.setArchitecture(inv.getArchitecture());
        this.setGuestOsType(inv.getGuestOsType());
        this.setVmEncryption(inv.getVmEncryption());
        this.setHasEncryptionResource(inv.getHasEncryptionResource());
        this.setKeyLastOpDate(inv.getKeyLastOpDate());
    }

    public VolumeInventory getRootVolume() {
        for (VolumeInventory v : allVolumes) {
            if (v.getUuid().equals(rootVolumeUuid)) {
                return v;
            }
        }

        return null;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    public Long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(Long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(Long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getLastHostUuid() {
        return lastHostUuid;
    }

    public void setLastHostUuid(String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public List<VmNicInventory> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<VmNicInventory> vmNics) {
        this.vmNics = vmNics;
    }

    /**
     * @return a list of disk the vm using on hypervisor, including root volume and data volume.
     * See :{@link VolumeType}
     * See :{@link VolumeInventory#isDisk}
     */
    public List<VolumeInventory> getAllDiskVolumes() {
        return getAllVolumes(VolumeInventory::isDisk);
    }

    @Deprecated
    public List<VolumeInventory> getAllVolumes() {
        return allVolumes;
    }

    public List<VolumeInventory> getAllVolumes(Predicate<VolumeInventory> predicate) {
        return allVolumes.stream().filter(predicate).collect(Collectors.toList());
    }

    public VolumeInventory getVolume(Predicate<VolumeInventory> predicate) {
        return allVolumes.stream().filter(predicate).findFirst().orElse(null);
    }

    public void setAllVolumes(List<VolumeInventory> allVolumes) {
        this.allVolumes = allVolumes;
    }

    public Long getInternalId() {
        return internalId;
    }

    public void setInternalId(Long internalId) {
        this.internalId = internalId;
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

    public Boolean getVmEncryption() {
        return vmEncryption;
    }

    public void setVmEncryption(Boolean vmEncryption) {
        this.vmEncryption = vmEncryption;
    }

    public Boolean getHasEncryptionResource() {
        return hasEncryptionResource;
    }

    public void setHasEncryptionResource(Boolean hasEncryptionResource) {
        this.hasEncryptionResource = hasEncryptionResource;
    }

    public Timestamp getKeyLastOpDate() {
        return keyLastOpDate;
    }

    public void setKeyLastOpDate(Timestamp keyLastOpDate) {
        this.keyLastOpDate = keyLastOpDate;
    }

    public List<VmCdRomInventory> getVmCdRoms() {
        return vmCdRoms;
    }

    public void setVmCdRoms(List<VmCdRomInventory> vmCdRoms) {
        this.vmCdRoms = vmCdRoms;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }
}
