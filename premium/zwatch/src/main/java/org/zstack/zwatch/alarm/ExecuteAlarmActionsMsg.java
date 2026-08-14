package org.zstack.zwatch.alarm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by kayo on 2018/10/10.
 */
public class ExecuteAlarmActionsMsg extends NeedReplyMessage implements AlarmMessage {
    private String alarmUuid;
    private AlarmStatus previousStatus;
    private AlarmStatus currentStatus;
    private Double currentValue;
    private String identifyLabel;

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

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public String getIdentifyLabel() {
        return identifyLabel;
    }

    public void setIdentifyLabel(String identifyLabel) {
        this.identifyLabel = identifyLabel;
    }
}
