package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class SyncImageFromImageStoreBackupStorageMsg extends NeedReplyMessage implements SyncImageFromImageStoreBackupStorageMessage {
    private String uuid;
    private String srcBackupStorageUuid;
    private String dstBackupStorageUuid;
    private String name;
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
