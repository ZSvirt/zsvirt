package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class SyncBackupStorageDataMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String srcBackupStorageUuid;
    private String dstBackupStorageUuid;

    @Override
    public String getBackupStorageUuid() {
        return srcBackupStorageUuid;
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
}
