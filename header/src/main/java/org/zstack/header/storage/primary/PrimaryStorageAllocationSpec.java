package org.zstack.header.storage.primary;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 */
public class PrimaryStorageAllocationSpec {
    private long size;
    private Long totalSize;
    private String requiredZoneUuid;
    private List<String> requiredClusterUuids;
    private String requiredHostUuid;
    private List<String> candidatePrimaryStorageUuids;
    private List<String> tags;
    private AllocatePrimaryStorageMsg allocationMessage;
    private String vmInstanceUuid;
    private String diskOfferingUuid;
    private List<String> avoidPrimaryStorageUuids;
    private String imageUuid;
    private boolean noOverProvisioning;
    private String purpose;
    private List<String> possiblePrimaryStorageTypes;
    private List<String> excludePrimaryStorageTypes;
    private String backupStorageUuid;
    private Set<PrimaryStorageFeature> requiredFeatures;

    public Set<PrimaryStorageFeature> getRequiredFeatures() {
        return requiredFeatures;
    }

    public void setRequiredFeatures(Set<PrimaryStorageFeature> requiredFeatures) {
        this.requiredFeatures = requiredFeatures;
    }

    public List<String> getCandidatePrimaryStorageUuids() {
        return candidatePrimaryStorageUuids == null ? Collections.emptyList() : candidatePrimaryStorageUuids;
    }

    public void setCandidatePrimaryStorageUuids(List<String> candidatePrimaryStorageUuids) {
        this.candidatePrimaryStorageUuids = candidatePrimaryStorageUuids;
    }

    public List<String> getExcludePrimaryStorageTypes() {
        return excludePrimaryStorageTypes;
    }

    public void setExcludePrimaryStorageTypes(List<String> excludePrimaryStorageTypes) {
        this.excludePrimaryStorageTypes = excludePrimaryStorageTypes;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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

    public List<String> getAvoidPrimaryStorageUuids() {
        return avoidPrimaryStorageUuids;
    }

    public void setAvoidPrimaryStorageUuids(List<String> avoidPrimaryStorageUuids) {
        this.avoidPrimaryStorageUuids = avoidPrimaryStorageUuids;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

    public AllocatePrimaryStorageMsg getAllocationMessage() {
        return allocationMessage;
    }

    public void setAllocationMessage(AllocatePrimaryStorageMsg allocationMessage) {
        this.allocationMessage = allocationMessage;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public long getTotalSize() {
        return totalSize != null ? totalSize : size;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }
}
