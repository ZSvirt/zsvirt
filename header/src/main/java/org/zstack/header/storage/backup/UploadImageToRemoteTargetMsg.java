package org.zstack.header.storage.backup;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;

public class UploadImageToRemoteTargetMsg extends NeedReplyMessage implements BackupStorageMessage {
    private ImageInventory image;
    private String backupStorageUuid;
    private String remoteTargetUrl;
    private String format;
    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public ImageInventory getImage() {
        return image;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getRemoteTargetUrl() {
        return remoteTargetUrl;
    }

    public void setRemoteTargetUrl(String remoteTargetUrl) {
        this.remoteTargetUrl = remoteTargetUrl;
    }
}
