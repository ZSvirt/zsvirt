package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: DaoDao
 * @Date: 2021/11/5
 */
public class GetImageEncryptedOnBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage{
    private String backupStorageUuid;
    private String imageUuid;

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
