package org.zstack.header.imagestore;

import org.zstack.header.message.NeedReplyMessage;

public class SyncDatabaseBackupBetweenImageStoreMsg extends NeedReplyMessage {
    private String databaseBackupUuid;
    private String srcImageStorageUuid;
    private String dstImageStorageUuid;
    private String newDatabaseBackupUuid;

    public String getDatabaseBackupUuid() {
        return databaseBackupUuid;
    }

    public void setDatabaseBackupUuid(String databaseBackupUuid) {
        this.databaseBackupUuid = databaseBackupUuid;
    }

    public String getSrcImageStorageUuid() {
        return srcImageStorageUuid;
    }

    public void setSrcImageStorageUuid(String srcImageStorageUuid) {
        this.srcImageStorageUuid = srcImageStorageUuid;
    }

    public String getDstImageStorageUuid() {
        return dstImageStorageUuid;
    }

    public void setDstImageStorageUuid(String dstImageStorageUuid) {
        this.dstImageStorageUuid = dstImageStorageUuid;
    }

    public String getNewDatabaseBackupUuid() {
        return newDatabaseBackupUuid;
    }

    public void setNewDatabaseBackupUuid(String newDatabaseBackupUuid) {
        this.newDatabaseBackupUuid = newDatabaseBackupUuid;
    }
}
