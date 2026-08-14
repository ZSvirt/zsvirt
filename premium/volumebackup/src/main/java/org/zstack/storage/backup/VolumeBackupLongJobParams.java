package org.zstack.storage.backup;

import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.storage.backup.BackupMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/3/8.
 */
public class VolumeBackupLongJobParams {
    private String volumeUuid;
    private List<String> alternativeBackupStorageUuids;
    private String name;
    private String accountUuid;
    private BackupQosStruct backupQosStruct;
    private BackupMode mode;

    /**
     * keep compatible with api message
     */
    private String backupStorageUuid;
    private String remoteBackupStorageUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getRemoteBackupStorageUuid() {
        return remoteBackupStorageUuid;
    }

    public void setRemoteBackupStorageUuid(String remoteBackupStorageUuid) {
        this.remoteBackupStorageUuid = remoteBackupStorageUuid;
    }
}
