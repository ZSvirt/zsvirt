package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.NewVmInstanceMessage;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneVO;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * Created by kayo on 2018/9/6.
 */
@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/vm-instances/from/vm-backups/{groupUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateVmFromVmBackupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APICreateVmFromVmBackupMsg extends APICreateMessage implements NewVmInstanceMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam
    private String groupUuid;

    @APIParam(resourceType = BackupStorageVO.class, required = false)
    private String backupStorageUuid;

    @APIParam(required = false, resourceType = InstanceOfferingVO.class)
    private String instanceOfferingUuid;

    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private List<String> l3NetworkUuids;

    @APIParam(required = false)
    private String vmNicParams;

    @APIParam(validValues = {"UserVm", "ApplianceVm"}, required = false)
    private String type;

    @APIParam(required = false, resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForRootVolume;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForDataVolume;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private List<String> rootVolumeSystemTags;

    @APIParam(required = false)
    private List<String> dataVolumeSystemTags;

    @APIParam(required = false)
    private List<DiskAO> diskAOs;

    public List<DiskAO> getDiskAOs() {
        return diskAOs;
    }

    public void setDiskAOs(List<DiskAO> diskAOs) {
        this.diskAOs = diskAOs;
    }

    @APIParam(required = false, validValues = {"InstantStart", "JustCreate", "CreateStopped"})
    private String strategy = VmCreationStrategy.InstantStart.toString();

    @APIParam(required = false)
    private Boolean resetTpm;

    @APIParam(required = false)
    private Integer cpuNum;

    @APIParam(required = false)
    private Long memorySize;

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long reservedMemorySize;

    @APINoSee
    private String platform;

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public List<String> getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(List<String> dataVolumeSystemTags) {
        this.dataVolumeSystemTags = dataVolumeSystemTags;
    }

    private String defaultL3NetworkUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
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

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getPrimaryStorageUuidForDataVolume() {
        return primaryStorageUuidForDataVolume;
    }

    public void setPrimaryStorageUuidForDataVolume(String primaryStorageUuidForDataVolume) {
        this.primaryStorageUuidForDataVolume = primaryStorageUuidForDataVolume;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Boolean getResetTpm() {
        return resetTpm;
    }

    public void setResetTpm(Boolean resetTpm) {
        this.resetTpm = resetTpm;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public static APICreateVmFromVmBackupMsg __example__() {
        APICreateVmFromVmBackupMsg msg = new APICreateVmFromVmBackupMsg();

        msg.setGroupUuid(uuid());
        msg.setName("vm1");
        msg.setDescription("this is a vm");
        msg.setClusterUuid(uuid());
        msg.setInstanceOfferingUuid(uuid());
        msg.setL3NetworkUuids(asList(uuid()));
        msg.setStrategy(VmCreationStrategy.InstantStart.toString());

        DiskAO disk = new DiskAO();
        disk.setSourceUuid(uuid());
        disk.setPrimaryStorageUuid(uuid());
        msg.setDiskAOs(asList(disk));

        return msg;
    }
}
