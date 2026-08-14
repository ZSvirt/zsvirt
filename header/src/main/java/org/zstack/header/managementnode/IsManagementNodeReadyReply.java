package org.zstack.header.managementnode;

import org.zstack.header.message.MessageReply;

/**
 */
public class IsManagementNodeReadyReply extends MessageReply {
    private boolean ready;

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
