package org.zstack.header.vpc.ha;

import org.zstack.header.message.DeletionMessage;

public class VpcHaGroupDeletionMsg extends DeletionMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
