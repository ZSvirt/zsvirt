package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/5/9.
 */
public class PingPrimaryStorageReply extends MessageReply {
    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
