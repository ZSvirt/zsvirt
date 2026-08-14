package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

public class UploadImageToRemoteTargetReply extends MessageReply {
    private String installPath;
    private long size;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
