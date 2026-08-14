package org.zstack.header.storage.backup;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Mei Lei on 16-12-20.
 */
public class BakeImageMetadataMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String metadata;
    private String backupStorageUuid;
    private String operation;
    private ImageInventory img;
    private String poolName;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public ImageInventory getImg() {
        return img;
    }

    public void setImg(ImageInventory img) {
        this.img = img;
    }


    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
