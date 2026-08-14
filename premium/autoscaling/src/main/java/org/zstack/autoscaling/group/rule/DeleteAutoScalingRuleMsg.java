package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/8.
 */
public class DeleteAutoScalingRuleMsg extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String ruleUuid;

    private String autoScalingGroupUuid;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }
}
