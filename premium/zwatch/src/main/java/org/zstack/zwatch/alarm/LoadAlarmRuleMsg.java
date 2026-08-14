package org.zstack.zwatch.alarm;

import org.zstack.header.message.NeedReplyMessage;

public class LoadAlarmRuleMsg extends NeedReplyMessage implements AlarmMessage {
    private String alarmUuid;

    @Override
    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}
