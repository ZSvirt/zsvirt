package org.zstack.autoscaling.group.activity;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2018/9/15.
 */
public class CreateAutoScalingGroupActivityReply extends MessageReply {
    private String autoScalingGroupActivityUuid;

    private MessageReply actionReply;

    public String getAutoScalingGroupActivityUuid() {
        return autoScalingGroupActivityUuid;
    }

    public void setAutoScalingGroupActivityUuid(String autoScalingGroupActivityUuid) {
        this.autoScalingGroupActivityUuid = autoScalingGroupActivityUuid;
    }

    public MessageReply getActionReply() {
        return actionReply;
    }

    public void setActionReply(MessageReply actionReply) {
        this.actionReply = actionReply;
    }
}
