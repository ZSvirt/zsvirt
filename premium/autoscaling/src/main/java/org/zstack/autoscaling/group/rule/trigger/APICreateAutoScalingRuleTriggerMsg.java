package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.autoscaling.group.rule.AutoScalingRuleVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * Create by lining at 2018/9/16
 */
public abstract class APICreateAutoScalingRuleTriggerMsg extends APICreateMessage {
    private String triggerType;

    @APIParam(maxLength = 256)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(resourceType = AutoScalingRuleVO.class)
    private String ruleUuid;

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
