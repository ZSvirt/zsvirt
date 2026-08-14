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
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.NewVmInstanceMessage;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneVO;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/vm-instances/from/vm-backup/{backupUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateVmFromVolumeBackupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APICreateVmFromVolumeBackupMsg extends APICreateMessage implements NewVmInstanceMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = VolumeBackupVO.class)
    private String backupUuid;

    @APIParam(resourceType = BackupStorageVO.class, required = false)
    private String backupStorageUuid;

    @APIParam(required = false, numberRange={1, Integer.MAX_VALUE})
    private Integer cpuNum;

    @APIParam(required = false, numberRange={1, Long.MAX_VALUE})
    private Long memorySize;

    @APIParam(resourceType = InstanceOfferingVO.class, required = false)
    private String instanceOfferingUuid;

    private String defaultL3NetworkUuid;

    @APIParam(resourceType = L3NetworkVO.class, nonempty = true)
    private List<String> l3NetworkUuids;

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

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private List<String> rootVolumeSystemTags;

    @APIParam(required = false, validValues = {"InstantStart", "JustCreate", "CreateStopped"})
    private String strategy = VmCreationStrategy.InstantStart.toString();

    @APIParam(required = false)
    private Boolean resetTpm;

    @APINoSee()
    private VolumeBackupVO volumeBackupVO;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    @Override
    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    @Override
    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    @Override
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

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public VolumeBackupVO getVolumeBackupVO() {
        return volumeBackupVO;
    }

    public void setVolumeBackupVO(VolumeBackupVO volumeBackupVO) {
        this.volumeBackupVO = volumeBackupVO;
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

    public static APICreateVmFromVolumeBackupMsg __example__() {
        APICreateVmFromVolumeBackupMsg msg = new APICreateVmFromVolumeBackupMsg();

        msg.setBackupUuid(uuid());
        msg.setName("vm1");
        msg.setDescription("this is a vm");
        msg.setClusterUuid(uuid());
        msg.setInstanceOfferingUuid(uuid());
        msg.setL3NetworkUuids(asList(uuid()));
        msg.setStrategy(VmCreationStrategy.InstantStart.toString());

        return msg;
    }
}
