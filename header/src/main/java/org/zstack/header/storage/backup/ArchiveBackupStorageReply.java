package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2019/12/2.
 */
public class ArchiveBackupStorageReply extends MessageReply {
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
