package org.zstack.header.storage.backup;

/**
 */
public class BackupStorageAllocationSpec {
    private long size;
    private String requiredBackupStorageUuid;
    private String requiredZoneUuid;
    private String requiredPrimaryStorageUuid;
    private AllocateBackupStorageMsg allocationMessage;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getRequiredBackupStorageUuid() {
        return requiredBackupStorageUuid;
    }

    public void setRequiredBackupStorageUuid(String requiredBackupStorageUuid) {
        this.requiredBackupStorageUuid = requiredBackupStorageUuid;
    }

    public AllocateBackupStorageMsg getAllocationMessage() {
        return allocationMessage;
    }

    public void setAllocationMessage(AllocateBackupStorageMsg allocationMessage) {
        this.allocationMessage = allocationMessage;
    }

    public String getRequiredZoneUuid() {
        return requiredZoneUuid;
    }

    public void setRequiredZoneUuid(String requiredZoneUuid) {
        this.requiredZoneUuid = requiredZoneUuid;
    }

    public String getRequiredPrimaryStorageUuid() {
        return requiredPrimaryStorageUuid;
    }

    public void setRequiredPrimaryStorageUuid(String requiredPrimaryStorageUuid) {
        this.requiredPrimaryStorageUuid = requiredPrimaryStorageUuid;
    }
}
