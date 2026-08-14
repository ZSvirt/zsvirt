package org.zstack.header.managementnode;

import org.zstack.header.message.NeedReplyMessage;

public class UpdateFactoryModeStateMsg extends NeedReplyMessage {
    private Boolean factoryModeState;

    public Boolean getFactoryModeState() {
        return factoryModeState;
    }

    public void setFactoryModeState(Boolean factoryModeState) {
        this.factoryModeState = factoryModeState;
    }
}
