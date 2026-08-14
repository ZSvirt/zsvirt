package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class GetImageDownloadProgressMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String imageUuid;
    private String hostname;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
