package org.zstack.scheduler;

import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.SchedulerTriggerInventory;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class CreateSchedulerTriggerReply extends MessageReply {
    private SchedulerTriggerInventory inventory;

    public SchedulerTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerTriggerInventory inventory) {
        this.inventory = inventory;
    }
}

