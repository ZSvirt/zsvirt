package org.zstack.storage.migration.backup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 * Created by GuoYi on 10/14/17.
 */
public class MigrateImageOnBackupStorageMsg extends NeedReplyMessage implements StorageMigrationMessage {
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
