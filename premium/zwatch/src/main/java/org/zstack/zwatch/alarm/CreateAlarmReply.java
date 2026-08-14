package org.zstack.zwatch.alarm;

import org.zstack.header.message.MessageReply;

/**
 * Created by ZStack on 2020/10/15.
 */
public class CreateAlarmReply extends MessageReply {
    private AlarmInventory inventory;

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
