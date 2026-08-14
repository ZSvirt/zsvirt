package org.zstack.header.storage.database.backup;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class DatabaseBackupDeletionMsg extends NeedReplyMessage implements DeleteDatabaseBackupMessage {
    private String uuid;
    private boolean dbOnly;
    private List<String> backupStorageUuids;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getDatabaseBackupUuid() {
        return uuid;
    }

    @Override
    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    @Override
    public boolean isDbOnly() {
        return dbOnly;
    }

    public void setDbOnly(boolean dbOnly) {
        this.dbOnly = dbOnly;
    }
}
