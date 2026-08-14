package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by kayo on 2018/7/25.
 */
public class VolumeBackupDeletionMsg extends NeedReplyMessage implements VolumeBackupDeleteMessage {
    private String uuid;
    private List<String> backupStorageUuids;
    private boolean dbOnly = false;
    private boolean handleDependency;

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeBackupUuid() {
        return uuid;
    }

    public boolean isDbOnly() {
        return dbOnly;
    }

    public void setDbOnly(boolean dbOnly) {
        this.dbOnly = dbOnly;
    }

    @Override
    public boolean isHandleDependency() {
        return handleDependency;
    }

    public void setHandleDependency(boolean handleDependency) {
        this.handleDependency = handleDependency;
    }
}
