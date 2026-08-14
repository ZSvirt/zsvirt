package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @api create a new vm instance
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APICreateVmInstanceMsg": {
 * "name": "TestVm",
 * "instanceOfferingUuid": "1618154b462a48749ca9b114cf4a2979",
 * "imageUuid": "99a5eea648954ef7be2b8ede8f34fe26",
 * "l3NetworkUuids": [
 * "c4f6a370f80443798cc460ee07d56ff1",
 * "f5fbd96e0df745bdb7bc4f4c19febe65",
 * "c60285dca24d43a4b9a2e536674ddca1"
 * ],
 * "type": "UserVm",
 * "dataDiskOfferingUuids": [],
 * "description": "Test",
 * "session": {
 * "uuid": "49c7e4c1fc18499a9477dd426436a8a4"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APICreateVmInstanceMsg": {
 * "name": "TestVm",
 * "instanceOfferingUuid": "1618154b462a48749ca9b114cf4a2979",
 * "imageUuid": "99a5eea648954ef7be2b8ede8f34fe26",
 * "l3NetworkUuids": [
 * "c4f6a370f80443798cc460ee07d56ff1",
 * "f5fbd96e0df745bdb7bc4f4c19febe65",
 * "c60285dca24d43a4b9a2e536674ddca1"
 * ],
 * "type": "UserVm",
 * "dataDiskOfferingUuids": [],
 * "description": "Test",
 * "session": {
 * "uuid": "49c7e4c1fc18499a9477dd426436a8a4"
 * },
 * "timeout": 1800000,
 * "id": "add5fb2198f14980adf26db572d035c5",
 * "serviceId": "api.portal",
 * "creatingTime": 1398912618016
 * }
 * }
 * @result See :ref:`APICreateVmInstanceEvent`
 * @since 0.1.0
 *
 * @summary 创建云主机
 */
@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/vm-instances",
        method = HttpMethod.POST,
        responseClass = APICreateVmInstanceEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APICreateVmInstanceMsg extends APICreateMessage implements APIAuditor, NewVmInstanceMessage2 {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * InstanceOfferingVO is deprecated, use cpuNum, memorySize, reservedMemorySize, allocatorStrategy instead
     */
    @Deprecated
    @APIParam(resourceType = InstanceOfferingVO.class, required = false)
    private String instanceOfferingUuid;

    @APIParam(required = false)
    private Integer cpuNum;

    @APIParam(required = false)
    private Long memorySize;

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long reservedMemorySize;

    /**
     * @desc uuid of image. See :ref:`ImageInventory`
     */
    @APIParam(resourceType = ImageVO.class, required = false, emptyString = false)
    private String imageUuid;
    /**
     * @desc a list of L3Network uuid the vm will create nic on. See :ref:`L3NetworkInventory`
     */
    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private List<String> l3NetworkUuids;

    @APIParam(required = false)
    private String vmNicParams;
    /**
     * @desc see type of :ref:`VmInstanceInventory`
     * @choices - UserVm
     * - ApplianceVm
     */
    @APIParam(validValues = {"UserVm", "ApplianceVm"}, required = false)
    private String type;
    /**
     * @desc disk offering uuid for root volume. Optional when vm is created from RootVolumeTemplate,
     * mandatory when vm is created from ISO. See 'mediaType' of :ref:`ImageInventory`
     * @optional
     */
    @Deprecated
    @APIParam(required = false, resourceType = DiskOfferingVO.class)
    private String rootDiskOfferingUuid;

    /**
     * use DiskAO.size
     */
    @Deprecated
    @APIParam(required = false)
    private Long rootDiskSize;

    /**
     * use DiskAO.size
     */
    @Deprecated
    @APIParam(required = false)
    private List<Long> dataDiskSizes;

    /**
     * @desc disk offering uuid for data volumes. See :ref:`DiskOfferingInventory`
     */
    @Deprecated
    @APIParam(required = false, resourceType = DiskOfferingVO.class)
    private List<String> dataDiskOfferingUuids;
    /**
     * @desc when not null, vm will be created in the zone this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = ZoneVO.class)
    private String zoneUuid;
    /**
     * @desc when not null, vm will be created in the cluster this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;
    /**
     * @desc when not null, vm will be created on the host this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;
    /**
     * @desc when not null, vm will be created on the primary storage this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForRootVolume;
    /**
     * @desc max length of 255 characters
     * @optional
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    private String defaultL3NetworkUuid;

    @APIParam(required = false, validValues = {"InstantStart", "JustCreate", "CreateStopped"})
    private String strategy = VmCreationStrategy.InstantStart.toString();

    @APIParam(required = false)
    private List<String> sshKeyPairUuids;

    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;

    @APIParam(required = false, maxLength = 255)
    private String guestOsType;

    @APIParam(required = false, maxLength = 32, validEnums = {CpuArchitecture.class})
    private String architecture;

    @APIParam(required = false)
    private Boolean virtio;

    @APIParam(required = false)
    private String allocatorStrategy;

    @APIParam(required = false)
    private List<DiskAO> diskAOs;

    @APIParam(required = false)
    private Map<String, Object> devices;

    /**
     * cache of {@link #devices}
     */
    @APINoSee
    private VmDevicesSpec devicesSpec;

    public List<DiskAO> getDiskAOs() {
        return diskAOs;
    }

    public void setDiskAOs(List<DiskAO> diskAOs) {
        this.diskAOs = diskAOs;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
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

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    @Override
    public String getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(String vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public Long getRootDiskSize() {
        return rootDiskSize;
    }

    public void setRootDiskSize(Long rootDiskSize) {
        this.rootDiskSize = rootDiskSize;
    }

    public List<Long> getDataDiskSizes() {
        return dataDiskSizes;
    }

    public void setDataDiskSizes(List<Long> dataDiskSizes) {
        this.dataDiskSizes = dataDiskSizes;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public List<String> getSshKeyPairUuids() {
        return sshKeyPairUuids;
    }

    public void setSshKeyPairUuids(List<String> sshKeyPairUuids) {
        this.sshKeyPairUuids = sshKeyPairUuids;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public Boolean getVirtio() {
        return virtio;
    }

    public void setVirtio(Boolean virtio) {
        this.virtio = virtio;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public Map<String, Object> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, Object> devices) {
        this.devices = devices;
    }

    public VmDevicesSpec getDevicesSpec() {
        if (devicesSpec == null && devices != null) {
            devicesSpec = JSONObjectUtil.rehashObject(devices, VmDevicesSpec.class);
        }
        return devicesSpec;
    }

    public void setDevicesSpec(VmDevicesSpec devicesSpec) {
        this.devicesSpec = devicesSpec;
    }

    @SuppressWarnings("unchecked")
    public static APICreateVmInstanceMsg __example__() {
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setName("vm1");
        msg.setDescription("this is a vm");
        msg.setClusterUuid(uuid());
        msg.setL3NetworkUuids(Collections.singletonList(uuid()));

        DiskAO disk1 = new DiskAO();
        disk1.setName("root-volume");
        disk1.setBoot(true);
        disk1.setPrimaryStorageUuid(uuid(PrimaryStorageVO.class));
        disk1.setPlatform("Linux");
        disk1.setGuestOsType("Helix 8");
        disk1.setArchitecture("x86_64");
        disk1.setSystemTags(list("volumeProvisioningStrategy::ThickProvisioning"));
        disk1.withVolume(uuid(VolumeVO.class));

        DiskAO disk2 = new DiskAO();
        disk2.setName("data-volume");
        disk2.setBoot(false);
        disk2.setPrimaryStorageUuid(uuid(PrimaryStorageVO.class));

        msg.setDiskAOs(list(disk1, disk2));
        msg.setDevices(JSONObjectUtil.rehashObject(VmDevicesSpec.__example__(), Map.class));

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVmInstanceEvent)rsp).getInventory().getUuid() : "", VmInstanceVO.class);
    }

}
