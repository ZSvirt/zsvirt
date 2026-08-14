package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 */
public class PingBackupStorageReply extends MessageReply {
    private boolean available;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
