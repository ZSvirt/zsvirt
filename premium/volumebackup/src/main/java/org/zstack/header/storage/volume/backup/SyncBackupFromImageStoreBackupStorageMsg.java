package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

public class SyncBackupFromImageStoreBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String uuid;
    private String srcBackupStorageUuid;
    private String dstBackupStorageUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }

    public String getDstBackupStorageUuid() {
        return dstBackupStorageUuid;
    }

    public void setDstBackupStorageUuid(String dstBackupStorageUuid) {
        this.dstBackupStorageUuid = dstBackupStorageUuid;
    }


    @Override
    public String getBackupStorageUuid() {
        return dstBackupStorageUuid;
    }
}
