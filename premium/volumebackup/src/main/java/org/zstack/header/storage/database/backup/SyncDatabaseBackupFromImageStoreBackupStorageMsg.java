package org.zstack.header.storage.database.backup;

import org.zstack.header.message.NeedReplyMessage;

public class SyncDatabaseBackupFromImageStoreBackupStorageMsg extends NeedReplyMessage implements SyncDatabaseBackupMessage {
    private String uuid;
    private String srcBackupStorageUuid;
    private String dstBackupStorageUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }

    @Override
    public String getDstBackupStorageUuid() {
        return dstBackupStorageUuid;
    }

    public void setDstBackupStorageUuid(String dstBackupStorageUuid) {
        this.dstBackupStorageUuid = dstBackupStorageUuid;
    }

    @Override
    public String getDatabaseBackupUuid() {
        return uuid;
    }
}
