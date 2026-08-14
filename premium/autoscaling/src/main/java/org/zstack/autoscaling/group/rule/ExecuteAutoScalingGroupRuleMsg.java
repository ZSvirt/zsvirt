package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/9/21.
 */
public class ExecuteAutoScalingGroupRuleMsg extends NeedReplyMessage implements AutoScalingRuleMsg, AutoScalingGroupMessage {
    private String autoScalingRuleUuid;

    private String autoScalingGroupUuid;

    @Override
    public String getAutoScalingRuleUuid() {
        return autoScalingRuleUuid;
    }

    public void setAutoScalingRuleUuid(String autoScalingRuleUuid) {
        this.autoScalingRuleUuid = autoScalingRuleUuid;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
