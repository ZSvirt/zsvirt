package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2018-10-15.
 */
public class PingBaremetalPxeServerReply extends MessageReply {
    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
