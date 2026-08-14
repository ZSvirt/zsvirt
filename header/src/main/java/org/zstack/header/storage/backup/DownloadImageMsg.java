package org.zstack.header.storage.backup;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class DownloadImageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private ImageInventory imageInventory;
    private String backupStorageUuid;
    private String format;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public DownloadImageMsg(ImageInventory inventory) {
        super();
        this.imageInventory = inventory;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

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
}
