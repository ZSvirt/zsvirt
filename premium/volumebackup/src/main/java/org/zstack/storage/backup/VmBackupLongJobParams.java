package org.zstack.storage.backup;

import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.storage.backup.BackupMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/3/8.
 */
public class VmBackupLongJobParams {
    private String rootVolumeUuid;
    private List<String> alternativeBackupStorageUuids;
    private String name;
    private String description;
    private String accountUuid;
    private BackupQosStruct backupQosStruct;
    private BackupMode mode;

    /**
     * keep compatible with api message
     */
    private String remoteBackupStorageUuid;
    private String backupStorageUuid;

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public List<String> getAlternativeBackupStorageUuids() {
        return alternativeBackupStorageUuids;
    }

    public void setAlternativeBackupStorageUuids(List<String> alternativeBackupStorageUuids) {
        this.alternativeBackupStorageUuids = alternativeBackupStorageUuids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public BackupQosStruct getBackupQosStruct() {
        return backupQosStruct;
    }

    public void setBackupQosStruct(BackupQosStruct backupQosStruct) {
        this.backupQosStruct = backupQosStruct;
    }

    public BackupMode getMode() {
        return mode;
    }

    public void setMode(BackupMode mode) {
        this.mode = mode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void makeCompatibleParams() {
        if (backupStorageUuid == null) {
            return;
        }

        if (alternativeBackupStorageUuids == null) {
            alternativeBackupStorageUuids = new ArrayList<>();
        }

        alternativeBackupStorageUuids.add(backupStorageUuid);

        if (mode == null) {
            mode = BackupMode.auto;
        }
    }

    public String getRemoteBackupStorageUuid() {
        return remoteBackupStorageUuid;
    }

    public void setRemoteBackupStorageUuid(String remoteBackupStorageUuid) {
        this.remoteBackupStorageUuid = remoteBackupStorageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
