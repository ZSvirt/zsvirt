package org.zstack.zwatch.alarm;

import org.zstack.header.message.MessageReply;

public class UpdateAlarmReply extends MessageReply {
    private AlarmInventory inventory;

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
