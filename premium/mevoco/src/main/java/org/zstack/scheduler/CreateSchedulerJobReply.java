package org.zstack.scheduler;

import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.SchedulerJobInventory;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class CreateSchedulerJobReply extends MessageReply {
    private SchedulerJobInventory inventory;

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
        this.inventory = inventory;
    }
}

