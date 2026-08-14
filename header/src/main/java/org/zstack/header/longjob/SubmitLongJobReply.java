package org.zstack.header.longjob;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 3/27/18
 */
public class SubmitLongJobReply extends MessageReply {
    private LongJobInventory inventory;
    private boolean needAudit = false;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public boolean isNeedAudit() {
        return needAudit;
    }

    public void setNeedAudit(boolean needAudit) {
        this.needAudit = needAudit;
    }
}
