package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/5/6.
 */
public class SyncImageSizeOnBackupStorageReply extends MessageReply {
    private long actualSize;
    private long size;

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
