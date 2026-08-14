package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

/**
 * Created by lining on 2018/9/21.
 */
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
public class TriggerAutoScalingGroupRuleMsg extends NeedReplyMessage implements AutoScalingRuleMsg, AutoScalingGroupMessage, ConfigurableTimeoutMessage {
    private String autoScalingRuleUuid;

    private String autoScalingRuleTriggerUUid;

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

    public String getAutoScalingRuleTriggerUUid() {
        return autoScalingRuleTriggerUUid;
    }

    public void setAutoScalingRuleTriggerUUid(String autoScalingRuleTriggerUUid) {
        this.autoScalingRuleTriggerUUid = autoScalingRuleTriggerUUid;
    }
}
