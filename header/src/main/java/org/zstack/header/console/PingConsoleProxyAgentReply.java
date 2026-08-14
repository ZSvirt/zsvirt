package org.zstack.header.console;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/4/8.
 */
public class PingConsoleProxyAgentReply extends MessageReply {
    private boolean connected;
    private boolean doReconnect;

    public boolean isDoReconnect() {
        return doReconnect;
    }

    public void setDoReconnect(boolean doReconnect) {
        this.doReconnect = doReconnect;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
