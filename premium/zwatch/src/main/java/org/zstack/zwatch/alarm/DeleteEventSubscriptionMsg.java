package org.zstack.zwatch.alarm;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteEventSubscriptionMsg extends NeedReplyMessage implements EventSubscriptionMessage {
    private String subscriptionUuid;
    private String managementNodeUuid;

    @Override
    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }
}
