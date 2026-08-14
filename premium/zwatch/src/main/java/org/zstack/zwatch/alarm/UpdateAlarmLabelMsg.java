package org.zstack.zwatch.alarm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/9/25.
 */
public class UpdateAlarmLabelMsg extends NeedReplyMessage implements AlarmMessage {
    private String uuid;

    private String alarmUuid;

    private String key;

    private String value;

    private String operator;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
