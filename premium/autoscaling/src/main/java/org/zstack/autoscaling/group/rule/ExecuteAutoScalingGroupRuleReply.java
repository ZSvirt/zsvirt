package org.zstack.autoscaling.group.rule;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2019/9/21.
 */
public class ExecuteAutoScalingGroupRuleReply extends MessageReply {
    private String autoScalingGroupActivityUuid;

    public String getAutoScalingGroupActivityUuid() {
        return autoScalingGroupActivityUuid;
    }

    public void setAutoScalingGroupActivityUuid(String autoScalingGroupActivityUuid) {
        this.autoScalingGroupActivityUuid = autoScalingGroupActivityUuid;
    }
}
