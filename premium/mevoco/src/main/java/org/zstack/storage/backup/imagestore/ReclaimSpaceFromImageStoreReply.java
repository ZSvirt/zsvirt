package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

/**
 * Created by david on 2/25/17.
 */
public class ReclaimSpaceFromImageStoreReply extends MessageReply {
    private long freedSpaceInBytes;

    public long getFreedSpaceInBytes() {
        return freedSpaceInBytes;
    }

    public void setFreedSpaceInBytes(long freedSpaceInBytes) {
        this.freedSpaceInBytes = freedSpaceInBytes;
    }
}
