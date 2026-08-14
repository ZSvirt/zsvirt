package org.zstack.header.cbt;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteCbtTaskMsg extends NeedReplyMessage {
    private String uuid;
    private boolean force = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
