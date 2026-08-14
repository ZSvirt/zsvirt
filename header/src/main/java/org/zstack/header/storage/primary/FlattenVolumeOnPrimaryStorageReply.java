package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class FlattenVolumeOnPrimaryStorageReply extends MessageReply {
    private long size;
    private long actualSize;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
