package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class RebaseVolumeBackingFileOnPrimaryStorageReply extends MessageReply {
    private int rebasedCount;

    public int getRebasedCount() {
        return rebasedCount;
    }

    public void setRebasedCount(int rebasedCount) {
        this.rebasedCount = rebasedCount;
    }
}
