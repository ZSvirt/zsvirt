package org.zstack.storage.primary;

import org.zstack.header.message.MessageReply;

public class EstimateVolumeTemplateSizeOnPrimaryStorageReply extends MessageReply {
    private long actualSize;
    private long size;
    private boolean withInternalSnapshot;

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

    public void setWithInternalSnapshot(boolean withInternalSnapshot) {
        this.withInternalSnapshot = withInternalSnapshot;
    }

    public boolean isWithInternalSnapshot() {
        return withInternalSnapshot;
    }
}
