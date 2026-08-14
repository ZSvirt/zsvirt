package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply extends MessageReply {
    private String installPath;
    private long size;
    private long actualSize;
    private String protocol;
    private boolean incremental;

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isIncremental() {
        return incremental;
    }
}
