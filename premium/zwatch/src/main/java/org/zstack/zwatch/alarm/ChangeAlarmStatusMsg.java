package org.zstack.zwatch.alarm;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeAlarmStatusMsg extends NeedReplyMessage implements AlarmMessage {
    private String alarmUuid;
    private AlarmStatus previousStatus;
    private AlarmStatus currentStatus;

    @Override
    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public AlarmStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(AlarmStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public AlarmStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(AlarmStatus currentStatus) {
        this.currentStatus = currentStatus;
    }
}
