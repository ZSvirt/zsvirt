package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class ConnectPrimaryStorageReply extends MessageReply {
    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
