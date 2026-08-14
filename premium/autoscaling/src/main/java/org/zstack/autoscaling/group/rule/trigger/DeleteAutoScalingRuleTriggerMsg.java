package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/8.
 */
public class DeleteAutoScalingRuleTriggerMsg extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String triggerUuid;

    private String autoScalingGroupUuid;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
