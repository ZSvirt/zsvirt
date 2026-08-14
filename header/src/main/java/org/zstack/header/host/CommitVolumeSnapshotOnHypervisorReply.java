package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class CommitVolumeSnapshotOnHypervisorReply extends MessageReply {
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
