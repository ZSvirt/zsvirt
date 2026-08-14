package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by miao on 16-7-11.
 */
public class GetImageSizeOnBackupStorageReply extends MessageReply {
    private long size;


    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
