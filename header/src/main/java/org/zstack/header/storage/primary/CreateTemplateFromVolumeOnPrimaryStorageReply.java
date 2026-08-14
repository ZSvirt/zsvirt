package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class CreateTemplateFromVolumeOnPrimaryStorageReply extends MessageReply {
    private String templateBackupStorageInstallPath;
    private String format;
    private long actualSize;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getTemplateBackupStorageInstallPath() {
        return templateBackupStorageInstallPath;
    }

    public void setTemplateBackupStorageInstallPath(String templateBackupStorageInstallPath) {
        this.templateBackupStorageInstallPath = templateBackupStorageInstallPath;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
