package org.zstack.storage.migration.backup;

import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

import java.util.concurrent.TimeUnit;

/**
 * Created by GuoYi on 12/7/17.
 *
 * This message is for BackupStorageMigrateImageJob
 */
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class BackupStorageMigrateImageMsg extends NeedReplyMessage implements StorageMigrationMessage, ConfigurableTimeoutMessage {
    private String imageUuid;
    private String srcBackupStorageUuid;
    private String dstBackupStorageUuid;
    private String type;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
