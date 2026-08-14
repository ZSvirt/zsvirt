package org.zstack.core.cloudbus;

import java.beans.ConstructorProperties;

/**
 */
public class WaitingReplyMessageStatistic {
    private String messageName;
    private long waitingTime;
    private String messageId;
    private String targetService;

    @ConstructorProperties({"messageName", "waitingTime", "messageId", "targetService"})
    public WaitingReplyMessageStatistic(String messageName, long waitingTime, String messageId, String targetService) {
        this.messageName = messageName;
        this.waitingTime = waitingTime;
        this.messageId = messageId;
        this.targetService = targetService;
    }

    public String getMessageName() {
        return messageName;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTargetService() {
        return targetService;
    }
}
