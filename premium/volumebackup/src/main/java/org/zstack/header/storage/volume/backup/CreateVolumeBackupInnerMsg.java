package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupMode;
import org.zstack.header.storage.backup.VolumeBackupMessage;
import org.zstack.storage.backup.BackupQosStruct;

public class CreateVolumeBackupInnerMsg extends NeedReplyMessage implements VolumeBackupMessage {
    private String resourceUuid;
    private String name;
    private String description;
    private String volumeUuid;
    private String backupStorageUuid;
    private String accountUuid;
    private BackupQosStruct backupQosStruct;
    private BackupMode mode;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public BackupQosStruct getBackupQosStruct() {
        return backupQosStruct;
    }

    public void setBackupQosStruct(BackupQosStruct backupQosStruct) {
        this.backupQosStruct = backupQosStruct;
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

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public BackupMode getMode() {
        return mode;
    }

    public void setMode(BackupMode mode) {
        this.mode = mode;
    }
}
