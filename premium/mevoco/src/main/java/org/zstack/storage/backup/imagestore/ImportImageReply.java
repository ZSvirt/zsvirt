package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

public class ImportImageReply extends MessageReply {
    private long size;
    private String installPath;

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
