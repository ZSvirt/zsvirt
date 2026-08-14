package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 */
public class DownloadVolumeReply extends MessageReply {
    private String installPath;
    private String md5sum;
    private long size;

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

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
