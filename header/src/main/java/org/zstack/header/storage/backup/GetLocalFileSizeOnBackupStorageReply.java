package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by camile on 2016/6/13.
 */
public class GetLocalFileSizeOnBackupStorageReply extends MessageReply {
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
