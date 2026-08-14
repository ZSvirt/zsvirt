package org.zstack.header.managementnode;

import org.zstack.header.message.MessageReply;

public class UpdateFactoryModeStateReply extends MessageReply {
    private boolean isNodeA = false;

    public boolean isNodeA() {
        return isNodeA;
    }

    public void setNodeA(boolean nodeA) {
        isNodeA = nodeA;
    }
}
