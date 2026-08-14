package org.zstack.zwatch.message;

import org.zstack.header.message.DeletionMessage;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateMessage;

public class SNSTextTemplateDeletionMsg extends DeletionMessage implements SNSTextTemplateMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAlarmTextTemplateUuid() {
        return uuid;
    }
}
