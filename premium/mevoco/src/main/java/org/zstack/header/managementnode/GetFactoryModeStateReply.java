package org.zstack.header.managementnode;

import org.zstack.header.message.MessageReply;

public class GetFactoryModeStateReply extends MessageReply {
    private Boolean factoryModeState;

    public Boolean getFactoryModeState() {
        return factoryModeState;
    }

    public void setFactoryModeState(Boolean factoryModeState) {
        this.factoryModeState = factoryModeState;
    }
}
