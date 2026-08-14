package org.zstack.scheduler;

import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.SchedulerJobSchedulerTriggerInventory;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class AddSchedulerJobToSchedulerTriggerReply extends MessageReply {
    private SchedulerJobSchedulerTriggerInventory  inventory;

    public SchedulerJobSchedulerTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobSchedulerTriggerInventory inventory) {
        this.inventory = inventory;
    }
}

