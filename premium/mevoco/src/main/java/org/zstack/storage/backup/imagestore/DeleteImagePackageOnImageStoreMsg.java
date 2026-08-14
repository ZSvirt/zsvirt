package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

/**
 * Created by Qi Le on 2022/5/3
 */
public class DeleteImagePackageOnImageStoreMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String exportUrl;

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
