package org.zstack.compute.vm;

import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmDevicesSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by xing5 on 2016/9/13.
 */
public class InstantiateVmFromNewCreatedStruct {
    private List<String> dataVolumeTemplateUuids;
    private Map<String, List<String>> dataVolumeFromTemplateSystemTags;
    private List<VmNicSpec> l3NetworkUuids;
    private String rootDiskOfferingUuid;
    private VmCreationStrategy strategy = VmCreationStrategy.InstantStart;
    private CpuArchitecture architecture;
    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private String requiredHostUuid;
    private List<String> softAvoidHostUuids;
    private List<String> avoidHostUuids;
    private List<String> disableL3Networks;
    private List<String> sshKeyPairUuids;
    private Map<String, Map<String, String>> vmNicSecurityPolicyByDeviceId = new HashMap<>();
    private final List<String> candidatePrimaryStorageUuidsForRootVolume = new ArrayList<>();
    private final List<String> candidatePrimaryStorageUuidsForDataVolume = new ArrayList<>();
    private DiskAO rootDisk;
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

    public static String makeLabelKey(String vmUuid) {
        return String.format("not-start-vm-%s", vmUuid);
    }

    public List<String> getDataVolumeTemplateUuids() {
        return dataVolumeTemplateUuids;
    }

    public void setDataVolumeTemplateUuids(List<String> dataVolumeTemplateUuids) {
        this.dataVolumeTemplateUuids = dataVolumeTemplateUuids;
    }

    public List<VmNicSpec> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<VmNicSpec> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public void setStrategy(VmCreationStrategy strategy) {
        this.strategy = strategy;
    }

    public VmCreationStrategy getStrategy() {
        return strategy;
    }

    public void setArchitecture(CpuArchitecture architecture) {
        this.architecture = architecture;
    }

    public CpuArchitecture getArchitecture() {
        return architecture;
    }

    public static InstantiateVmFromNewCreatedStruct fromMessage(InstantiateNewCreatedVmInstanceMsg msg) {
        InstantiateVmFromNewCreatedStruct struct = new InstantiateVmFromNewCreatedStruct();
        struct.setDataVolumeTemplateUuids(msg.getDataVolumeTemplateUuids());
        struct.setDataVolumeFromTemplateSystemTags(msg.getDataVolumeFromTemplateSystemTags());
        struct.setL3NetworkUuids(msg.getL3NetworkUuids());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setCandidatePrimaryStorageUuidsForRootVolume(msg.getCandidatePrimaryStorageUuidsForRootVolume());
        struct.setCandidatePrimaryStorageUuidsForDataVolume(msg.getCandidatePrimaryStorageUuidsForDataVolume());
        struct.strategy = VmCreationStrategy.valueOf(msg.getStrategy());
        struct.architecture = msg.getArchitecture();
        struct.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
        struct.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
        struct.setRequiredHostUuid(msg.getHostUuid());
        struct.setSoftAvoidHostUuids(msg.getSoftAvoidHostUuids());
        struct.setAvoidHostUuids(msg.getAvoidHostUuids());
        struct.setDisableL3Networks(msg.getDisableL3Networks());
        struct.setRootDisk(msg.getRootDisk());
        struct.setDataDisks(msg.getDataDisks());
        struct.setDeprecatedDataVolumeSpecs(msg.getDeprecatedDataVolumeSpecs());
        struct.setVmCustomSpecification(msg.getVmCustomSpecification());
        struct.setDevicesSpec(msg.getDevicesSpec());
        struct.setVmNicSecurityPolicyByDeviceId(msg.getVmNicSecurityPolicyByDeviceId());
        return struct;
    }

    public static InstantiateVmFromNewCreatedStruct fromMessage(CreateVmInstanceMsg msg) {
        InstantiateVmFromNewCreatedStruct struct = new InstantiateVmFromNewCreatedStruct();
        struct.setDataVolumeTemplateUuids(msg.getDataVolumeTemplateUuids());
        struct.setDataVolumeFromTemplateSystemTags(msg.getDataVolumeFromTemplateSystemTags());
        struct.setL3NetworkUuids(msg.getL3NetworkSpecs());
        struct.setCandidatePrimaryStorageUuidsForRootVolume(msg.getCandidatePrimaryStorageUuidsForRootVolume());
        struct.setCandidatePrimaryStorageUuidsForDataVolume(msg.getCandidatePrimaryStorageUuidsForDataVolume());
        struct.strategy = VmCreationStrategy.valueOf(msg.getStrategy());
        struct.architecture = msg.getArchitecture() == null ? null : CpuArchitecture.valueOf(msg.getArchitecture());
        struct.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
        struct.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
        struct.setRequiredHostUuid(msg.getHostUuid());
        struct.setDisableL3Networks(msg.getDisableL3Networks());
        struct.setRootDisk(msg.getRootDisk());
        struct.setDataDisks(msg.getDataDisks());
        struct.setDeprecatedDataVolumeSpecs(msg.getDeprecatedDataVolumeSpecs());
        struct.setVmCustomSpecification(msg.getVmCustomSpecification());
        struct.setDevicesSpec(msg.getDevicesSpec());
        struct.setVmNicSecurityPolicyByDeviceId(msg.getVmNicSecurityPolicyByDeviceId());
        return struct;
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

    public String getRequiredHostUuid() {
        return requiredHostUuid;
    }

    public void setRequiredHostUuid(String requiredHostUuid) {
        this.requiredHostUuid = requiredHostUuid;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public Map<String, List<String>> getDataVolumeFromTemplateSystemTags() {
        return dataVolumeFromTemplateSystemTags;
    }

    public void setDataVolumeFromTemplateSystemTags(Map<String, List<String>> dataVolumeFromTemplateSystemTags) {
        this.dataVolumeFromTemplateSystemTags = dataVolumeFromTemplateSystemTags;
    }

    @Deprecated
    public void setDataVolumeSystemTagsOnIndex(Map<String, List<String>> dataVolumeSystemTagsOnIndex) {
        if (dataVolumeSystemTagsOnIndex == null) {
            return;
        }

        int maxIndex = dataVolumeSystemTagsOnIndex.keySet().stream().mapToInt(Integer::parseInt).max().orElse(-1);
        for (int i = deprecatedDataVolumeSpecs.size(); i <= maxIndex; i++) {
            deprecatedDataVolumeSpecs.add(DiskAO.nonRootDisk());
        }

        for (int i = 0; i <= maxIndex; i++) {
            String key = i + "";
            if (!dataVolumeSystemTagsOnIndex.containsKey(key)) {
                continue;
            }

            final DiskAO diskAO = deprecatedDataVolumeSpecs.get(i);
            if (diskAO.getSystemTags() == null) {
                diskAO.setSystemTags(new ArrayList<>());
            }
            diskAO.getSystemTags().addAll(dataVolumeSystemTagsOnIndex.get(key));
        }
    }

    public List<String> getDisableL3Networks() {
        return disableL3Networks;
    }

    public void setDisableL3Networks(List<String> disableL3Networks) {
        this.disableL3Networks = disableL3Networks;
    }

    public Map<String, Map<String, String>> getVmNicSecurityPolicyByDeviceId() {
        if (vmNicSecurityPolicyByDeviceId == null) {
            vmNicSecurityPolicyByDeviceId = new HashMap<>();
        }
        return vmNicSecurityPolicyByDeviceId;
    }

    public void setVmNicSecurityPolicyByDeviceId(Map<String, Map<String, String>> vmNicSecurityPolicyByDeviceId) {
        this.vmNicSecurityPolicyByDeviceId = vmNicSecurityPolicyByDeviceId;
    }

    public List<String> getSshKeyPairUuids() {
        return sshKeyPairUuids;
    }

    public void setSshKeyPairUuids(List<String> sshKeyPairUuids) {
        this.sshKeyPairUuids = sshKeyPairUuids;
    }
}
