package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupMode;
import org.zstack.header.storage.backup.VolumeBackupMessage;
import org.zstack.storage.backup.BackupQosStruct;

import java.util.Set;

public class CreateVmBackupOverlayInnerMsg extends NeedReplyMessage implements VolumeBackupMessage {
    private String name;
    private String description;
    private String hostUuid;
    private String vmInstanceUuid;
    private String accountUuid;
    private String volumeUuid; // current volume uuid
    private String backupStorageUuid;
    private String resourceUuid;
    private Set<String> volumeUuids;
    private Set<String> lockedVolumeUuids;
    private BackupQosStruct backupQosStruct;
    private BackupMode mode;

    public BackupQosStruct getBackupQosStruct() {
        return backupQosStruct;
    }

    public void setBackupQosStruct(BackupQosStruct backupQosStruct) {
        this.backupQosStruct = backupQosStruct;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Set<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(Set<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public Set<String> getLockedVolumeUuids() {
        return lockedVolumeUuids;
    }

    public void setLockedVolumeUuids(Set<String> lockedVolumeUuids) {
        this.lockedVolumeUuids = lockedVolumeUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public BackupMode getMode() {
        return mode;
    }

    public void setMode(BackupMode mode) {
        this.mode = mode;
    }
}
