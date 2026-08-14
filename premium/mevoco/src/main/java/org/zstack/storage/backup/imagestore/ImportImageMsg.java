package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

public class ImportImageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;
    private String name;
    private String description;
    private String parent;
    private String filename;
    private String processToRelease;

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
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

    public String getProcessToRelease() {
        return processToRelease;
    }

    public void setProcessToRelease(String processToRelease) {
        this.processToRelease = processToRelease;
    }
}
