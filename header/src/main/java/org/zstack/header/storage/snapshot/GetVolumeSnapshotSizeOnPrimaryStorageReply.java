package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2019/5/14.
 */
public class GetVolumeSnapshotSizeOnPrimaryStorageReply extends MessageReply {
    private Long size;

    private Long actualSize;

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }
}
