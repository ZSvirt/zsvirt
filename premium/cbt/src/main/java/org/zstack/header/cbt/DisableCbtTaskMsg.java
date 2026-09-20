package org.zstack.header.cbt;

import org.zstack.header.message.NeedReplyMessage;

public class DisableCbtTaskMsg extends NeedReplyMessage {
    private String uuid;
    private boolean force = false;
    private boolean fromVmStateChange;

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

    public boolean isFromVmStateChange() {
        return fromVmStateChange;
    }

    public void setFromVmStateChange(boolean fromVmStateChange) {
        this.fromVmStateChange = fromVmStateChange;
    }
}
