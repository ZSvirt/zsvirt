package org.zstack.header.image;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/5/6.
 */
public class SyncImageSizeMsg extends NeedReplyMessage implements ImageMessage {
    private String imageUuid;
    private String backupStorageUuid;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
