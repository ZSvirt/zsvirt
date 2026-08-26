package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.CollectionDSL;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Allocate PrimaryStorage
 * * @deprecated
 * * This method is no longer acceptable to allocate PrimaryStorage.
 * * <p> Use {@link AllocatePrimaryStorageSpaceMsg} instead.
 */
@Deprecated
public class AllocatePrimaryStorageMsg extends NeedReplyMessage {
    private String requiredZoneUuid;
    private List<String> requiredClusterUuids;
    private String requiredHostUuid;
    private final List<String> candidatePrimaryStorageUuids = new ArrayList<>();
    private String backupStorageUuid;
    private List<String> possiblePrimaryStorageTypes;
    private List<String> excludePrimaryStorageTypes;
    private Set<PrimaryStorageFeature> requiredFeatures;

    private Long totalSize = null;
    private long size;
    private boolean dryRun;
    private List<String> tags;
    private String allocationStrategy;
    private String vmInstanceUuid;
    @Nullable
    private String diskOfferingUuid;
    private List<String> excludePrimaryStorageUuids;
    private List<String> excludeAllocatorStrategies;
    private String imageUuid;
    /**
     * Allocate PrimaryStorage
     * * @deprecated
     * * This parameter is no longer acceptable to allocate PrimaryStorage.
     * * <p> Use msg.setRequiredInstallUri("volume://volumeUuid") instead.
     */
    @Deprecated
    private String volumeUuid;
    private boolean noOverProvisioning;
    private String purpose;

    public Set<PrimaryStorageFeature> getRequiredFeatures() {
        return requiredFeatures;
    }

    public void setRequiredFeatures(Set<PrimaryStorageFeature> requiredFeatures) {
        this.requiredFeatures = requiredFeatures;
    }

    public void addRequiredFeature(PrimaryStorageFeature requiredFeature) {
        requiredFeatures = requiredFeatures == null || requiredFeatures.isEmpty()
                ? EnumSet.of(requiredFeature)
                : EnumSet.copyOf(requiredFeatures);
        requiredFeatures.add(requiredFeature);
    }

    public List<String> getExcludePrimaryStorageTypes() {
        return excludePrimaryStorageTypes;
    }

    public void addExcludePrimaryStorageTypes(List<String> excludePrimaryStorageTypes) {
        if (this.excludePrimaryStorageTypes == null) {
            this.excludePrimaryStorageTypes = CollectionDSL.list();
        }
        this.excludePrimaryStorageTypes.addAll(excludePrimaryStorageTypes);
    }

    public void setExcludePrimaryStorageTypes(List<String> excludePrimaryStorageTypes) {
        this.excludePrimaryStorageTypes = excludePrimaryStorageTypes;
    }

    public List<String> getPossiblePrimaryStorageTypes() {
        return possiblePrimaryStorageTypes;
    }

    public void setPossiblePrimaryStorageTypes(List<String> possiblePrimaryStorageTypes) {
        this.possiblePrimaryStorageTypes = possiblePrimaryStorageTypes;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public boolean isNoOverProvisioning() {
        return noOverProvisioning;
    }

    public void setNoOverProvisioning(boolean noOverProvisioning) {
        this.noOverProvisioning = noOverProvisioning;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getExcludeAllocatorStrategies() {
        return excludeAllocatorStrategies;
    }

    public void setExcludeAllocatorStrategies(List<String> excludeAllocatorStrategies) {
        this.excludeAllocatorStrategies = excludeAllocatorStrategies;
    }

    public void addExcludeAllocatorStrategy(String allocationStrategy) {
        if (excludeAllocatorStrategies == null) {
            excludeAllocatorStrategies = new ArrayList<>();
        }
        excludeAllocatorStrategies.add(allocationStrategy);
    }

    public List<String> getExcludePrimaryStorageUuids() {
        return excludePrimaryStorageUuids;
    }

    public void setExcludePrimaryStorageUuids(List<String> excludePrimaryStorageUuids) {
        this.excludePrimaryStorageUuids = excludePrimaryStorageUuids;
    }

    public void addExcludePrimaryStorageUuid(String priUuid) {
        if (excludePrimaryStorageUuids == null) {
            excludePrimaryStorageUuids = new ArrayList<String>();
        }

        excludePrimaryStorageUuids.add(priUuid);
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }

    @Deprecated
    public String getRequiredPrimaryStorageUuid() {
        return this.candidatePrimaryStorageUuids.isEmpty() ? null : this.candidatePrimaryStorageUuids.get(0);
    }

    public void setRequiredPrimaryStorageUuid(String requiredPrimaryStorageUuid) {
        this.candidatePrimaryStorageUuids.clear();
        if (requiredPrimaryStorageUuid != null) {
            this.candidatePrimaryStorageUuids.add(requiredPrimaryStorageUuid);
        }
    }

    public List<String> getCandidatePrimaryStorageUuids() {
        return candidatePrimaryStorageUuids;
    }

    public void setCandidatePrimaryStorageUuids(List<String> candidatePrimaryStorageUuids) {
        this.candidatePrimaryStorageUuids.clear();
        if (candidatePrimaryStorageUuids != null) {
            this.candidatePrimaryStorageUuids.addAll(candidatePrimaryStorageUuids);
        }
    }

    public String getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public String getRequiredZoneUuid() {
        return requiredZoneUuid;
    }

    public void setRequiredZoneUuid(String requiredZoneUuid) {
        this.requiredZoneUuid = requiredZoneUuid;
    }

    public List<String> getRequiredClusterUuids() {
        return requiredClusterUuids;
    }

    public void setRequiredClusterUuids(List<String> requiredClusterUuids) {
        this.requiredClusterUuids = requiredClusterUuids;
    }

    public String getRequiredHostUuid() {
        return requiredHostUuid;
    }

    public void setRequiredHostUuid(String requiredHostUuid) {
        this.requiredHostUuid = requiredHostUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
