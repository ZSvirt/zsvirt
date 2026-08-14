package org.zstack.header.vm;

import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.devices.VmDevicesSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by david on 8/4/16.
 */
public class CreateVmInstanceMsg extends NeedReplyMessage implements CreateVmInstanceMessage {
    private String accountUuid;
    private String name;
    private String imageUuid;
    private String instanceOfferingUuid;
    private int cpuNum;
    private long cpuSpeed;
    private long memorySize;
    private long reservedMemorySize;
    private List<VmNicSpec> l3NetworkSpecs;
    private String type;
    private List<String> dataVolumeTemplateUuids;
    private Map<String, List<String>> dataVolumeFromTemplateSystemTags;
    private String zoneUuid;
    private String clusterUuid;
    private String hostUuid;
    private String description;
    private String resourceUuid;
    private String defaultL3NetworkUuid;
    private String allocatorStrategy;
    private String strategy;
    private String platform;
    private String guestOsType;
    private String architecture;
    private Boolean virtio;
    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private List<String> disableL3Networks;
    private List<String> sshKeyPairUuids;
    private final List<String> candidatePrimaryStorageUuidsForRootVolume = new ArrayList<>();
    private final List<String> candidatePrimaryStorageUuidsForDataVolume = new ArrayList<>();
    private DiskAO rootDisk = DiskAO.rootDisk();
    private List<DiskAO> dataDisks;
    private List<DiskAO> deprecatedDataVolumeSpecs = new ArrayList<>();
    private VmCustomSpecificationStruct vmCustomSpecification;
    private VmDevicesSpec devicesSpec;

    public List<String> getCandidatePrimaryStorageUuidsForRootVolume() {
        return candidatePrimaryStorageUuidsForRootVolume;
    }

    public void setCandidatePrimaryStorageUuidsForRootVolume(List<String> candidatePrimaryStorageUuidsForRootVolume) {
        this.candidatePrimaryStorageUuidsForRootVolume.clear();
        if (candidatePrimaryStorageUuidsForRootVolume != null) {
            this.candidatePrimaryStorageUuidsForRootVolume.addAll(candidatePrimaryStorageUuidsForRootVolume);
        }
    }

    public List<String> getCandidatePrimaryStorageUuidsForDataVolume() {
        return candidatePrimaryStorageUuidsForDataVolume;
    }

    public void setCandidatePrimaryStorageUuidsForDataVolume(List<String> candidatePrimaryStorageUuidsForDataVolume) {
        this.candidatePrimaryStorageUuidsForDataVolume.clear();
        if (candidatePrimaryStorageUuidsForDataVolume != null) {
            this.candidatePrimaryStorageUuidsForDataVolume.addAll(candidatePrimaryStorageUuidsForDataVolume);
        }
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public DiskAO getRootDisk() {
        return rootDisk;
    }

    public void setRootDisk(DiskAO rootDisk) {
        this.rootDisk = rootDisk;
    }

    public List<DiskAO> getDataDisks() {
        return dataDisks;
    }

    public void setDataDisks(List<DiskAO> dataDisks) {
        this.dataDisks = dataDisks;
    }

    public List<DiskAO> getDeprecatedDataVolumeSpecs() {
        return deprecatedDataVolumeSpecs;
    }

    public void setDeprecatedDataVolumeSpecs(List<DiskAO> deprecatedDataVolumeSpecs) {
        this.deprecatedDataVolumeSpecs = deprecatedDataVolumeSpecs;
    }

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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @Override
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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<VmNicSpec> getL3NetworkSpecs() {
        return l3NetworkSpecs;
    }

    public void setL3NetworkSpecs(List<VmNicSpec> l3NetworkSpecs) {
        this.l3NetworkSpecs = l3NetworkSpecs;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    @Override
    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    @Override
    public long getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    @Override
    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    @Override
    public long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    @Override
    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Deprecated
    public String getPrimaryStorageUuidForRootVolume() {
        return this.candidatePrimaryStorageUuidsForRootVolume.isEmpty() ? null : this.candidatePrimaryStorageUuidsForRootVolume.get(0);
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.candidatePrimaryStorageUuidsForRootVolume.clear();
        if (primaryStorageUuidForRootVolume != null) {
            this.candidatePrimaryStorageUuidsForRootVolume.add(primaryStorageUuidForRootVolume);
        }
    }

    @Deprecated
    public String getPrimaryStorageUuidForDataVolume() {
        return this.candidatePrimaryStorageUuidsForDataVolume.isEmpty() ? null : this.candidatePrimaryStorageUuidsForDataVolume.get(0);
    }

    public void setPrimaryStorageUuidForDataVolume(String primaryStorageUuidForDataVolume) {
        this.candidatePrimaryStorageUuidsForDataVolume.clear();
        if (primaryStorageUuidForDataVolume != null) {
            this.candidatePrimaryStorageUuidsForDataVolume.add(primaryStorageUuidForDataVolume);
        }
    }

    public List<String> getDataVolumeTemplateUuids() {
        return dataVolumeTemplateUuids;
    }

    public void setDataVolumeTemplateUuids(List<String> dataVolumeTemplateUuids) {
        this.dataVolumeTemplateUuids = dataVolumeTemplateUuids;
    }

    public Map<String, List<String>> getDataVolumeFromTemplateSystemTags() {
        return dataVolumeFromTemplateSystemTags;
    }

    public void setDataVolumeFromTemplateSystemTags(Map<String, List<String>> dataVolumeFromTemplateSystemTags) {
        this.dataVolumeFromTemplateSystemTags = dataVolumeFromTemplateSystemTags;
    }

    public List<String> getDisableL3Networks() {
        return disableL3Networks;
    }

    public void setDisableL3Networks(List<String> disableL3Networks) {
        this.disableL3Networks = disableL3Networks;
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

    public VmCustomSpecificationStruct getVmCustomSpecification() {
        return vmCustomSpecification;
    }

    public void setVmCustomSpecification(VmCustomSpecificationStruct vmCustomSpecification) {
        this.vmCustomSpecification = vmCustomSpecification;
    }

    public VmDevicesSpec getDevicesSpec() {
        return devicesSpec;
    }

    public void setDevicesSpec(VmDevicesSpec devicesSpec) {
        this.devicesSpec = devicesSpec;
    }
}
