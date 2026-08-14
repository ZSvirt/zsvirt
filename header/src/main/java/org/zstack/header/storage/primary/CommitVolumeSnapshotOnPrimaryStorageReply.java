package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class CommitVolumeSnapshotOnPrimaryStorageReply extends MessageReply {
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
