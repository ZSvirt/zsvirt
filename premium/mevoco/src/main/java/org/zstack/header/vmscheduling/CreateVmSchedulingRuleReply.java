package org.zstack.header.vmscheduling;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2022/11/29
 */
public class CreateVmSchedulingRuleReply extends MessageReply {
    private VmSchedulingRuleInventory inventory;

    public VmSchedulingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmSchedulingRuleInventory inventory) {
        this.inventory = inventory;
    }
}
