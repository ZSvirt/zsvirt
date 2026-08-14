package org.zstack.header.network.l3;

import org.zstack.header.message.DeletionMessage;

public class DeleteL3NetworkMsg extends DeletionMessage implements L3NetworkMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getL3NetworkUuid() {
        return uuid;
    }
}
