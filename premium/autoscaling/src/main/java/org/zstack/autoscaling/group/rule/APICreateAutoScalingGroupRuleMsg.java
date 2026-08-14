package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;

/**
 * Create by lining at 2018/9/11
 */
public abstract class APICreateAutoScalingGroupRuleMsg extends APICreateMessage implements APIAuditor, AutoScalingGroupMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(resourceType = AutoScalingGroupVO.class)
    private String autoScalingGroupUuid;

    private String type;

    @APIParam(required = false, numberRange = {1, Integer.MAX_VALUE})
    private Long cooldown;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getCooldown() {
        return cooldown;
    }

    public void setCooldown(Long cooldown) {
        this.cooldown = cooldown;
    }

    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateAutoScalingRuleEvent)rsp).getInventory().getUuid() : "", AutoScalingRuleVO.class);
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
