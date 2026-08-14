package org.zstack.header.storage.database.backup;

import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(value = 3, timeunit = TimeUnit.HOURS)
public class CreateDatabaseBackupMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String backupStorageUuid;

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
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
