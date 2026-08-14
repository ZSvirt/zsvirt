package org.zstack.header.storage.backup;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

public class UploadFileToBackupStorageHostReply extends MessageReply {
    private String md5sum;
    private long size;
    private String format;
    @NoLogging(type = NoLogging.Type.Uri)
    private String directUploadUrl;
    private String backupStorageHostUuid;

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDirectUploadUrl() {
        return directUploadUrl;
    }

    public void setDirectUploadUrl(String directUploadUrl) {
        this.directUploadUrl = directUploadUrl;
    }

    public String getBackupStorageHostUuid() {
        return backupStorageHostUuid;
    }

    public void setBackupStorageHostUuid(String backupStorageHostUuid) {
        this.backupStorageHostUuid = backupStorageHostUuid;
    }
}
