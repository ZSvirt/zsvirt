package org.zstack.zwatch.alarm;


import org.zstack.header.message.NeedReplyMessage;

public class ChangeAlarmStateMsg extends NeedReplyMessage implements AlarmMessage{
    private String uuid;
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public AlarmStateEvent getStateEvent() {
        return AlarmStateEvent.valueOf(stateEvent);
    }

    public void setStateEvent(AlarmStateEvent stateEvent) {
        this.stateEvent = stateEvent.toString();
    }

    @Override
    public String getAlarmUuid() {
        return uuid;
    }
}
