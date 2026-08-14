package org.zstack.zwatch.message;

import org.zstack.header.message.DeletionMessage;
import org.zstack.zwatch.alarm.AlarmMessage;

public class AlarmDeletionMsg extends DeletionMessage implements AlarmMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAlarmUuid() {
        return uuid;
    }
}
