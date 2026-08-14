package org.zstack.autoscaling.group.rule;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@AutoQuery(replyClass = APIUpdateAutoScalingRuleEvent.class, inventoryClass = AutoScalingRuleInventory.class)
@RestRequest(
        path = "/autoscaling/rules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAutoScalingRuleEvent.class,
        isAction = true
)
public class APIUpdateAutoScalingRuleMsg extends APIMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingRuleVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(numberRange = {1,900}, required = false)
    private Long cooldown;

    @APINoSee
    private String autoScalingGroupUuid;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public static APIUpdateAutoScalingRuleMsg __example__() {
        APIUpdateAutoScalingRuleMsg msg = new APIUpdateAutoScalingRuleMsg();
        msg.setUuid(uuid());
        msg.setName("test name2");
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public Long getCooldown() {
        return cooldown;
    }

    public void setCooldown(Long cooldown) {
        this.cooldown = cooldown;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
