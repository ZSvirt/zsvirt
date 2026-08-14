package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:53 2023/11/10
 */
public class CalculateImageHashOnBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String imageUuid;
    private String algorithm;

    private String backupStorageUuid;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
