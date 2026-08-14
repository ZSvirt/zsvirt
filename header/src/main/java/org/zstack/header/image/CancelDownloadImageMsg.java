package org.zstack.header.image;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

/**
 * Created by MaJin on 2019/7/12.
 */
public class CancelDownloadImageMsg extends CancelMessage implements BackupStorageMessage {
    private ImageInventory imageInventory;
    private String backupStorageUuid;
    private String format;

    public ImageInventory getImageInventory() {
        return imageInventory;
    }

    public void setImageInventory(ImageInventory imageInventory) {
        this.imageInventory = imageInventory;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
