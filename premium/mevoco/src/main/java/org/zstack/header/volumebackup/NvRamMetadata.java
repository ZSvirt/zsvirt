package org.zstack.header.volumebackup;

/**
 * NvRam metadata attached to a volume backup. Stored as JSON in VolumeBackupMetadata.attachments
 */
public class NvRamMetadata {
    private String uuid;
    private String contentBase64;
    private String contentFormat;
    private long createDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getContentFormat() {
        return contentFormat;
    }

    public void setContentFormat(String contentFormat) {
        this.contentFormat = contentFormat;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }
}
