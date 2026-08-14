package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 */
public class ChangeVmMetaDataReply extends MessageReply {
    private boolean changeHostUuidDone;
    private boolean changeStateDone;

    public boolean isChangeHostUuidDone() {
        return changeHostUuidDone;
    }

    public void setChangeHostUuidDone(boolean changeHostUuidDone) {
        this.changeHostUuidDone = changeHostUuidDone;
    }

    public boolean isChangeStateDone() {
        return changeStateDone;
    }

    public void setChangeStateDone(boolean changeStateDone) {
        this.changeStateDone = changeStateDone;
    }
}
