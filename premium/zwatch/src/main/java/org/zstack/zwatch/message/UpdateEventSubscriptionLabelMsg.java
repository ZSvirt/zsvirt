package org.zstack.zwatch.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.zwatch.alarm.EventSubscriptionMessage;

/**
 * Created by lining on 2018/9/25.
 */
public class UpdateEventSubscriptionLabelMsg extends NeedReplyMessage implements EventSubscriptionMessage {
    private String uuid;

    private String subscriptionUuid;

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
    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
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
