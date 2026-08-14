package org.zstack.autoscaling.group;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Create by weiwang at 2018/8/16
 */
@TagResourceType(AutoScalingGroupVO.class)
@RestRequest(
        path = "/autoscaling/groups",
        method = HttpMethod.POST,
        responseClass = APICreateAutoScalingGroupEvent.class,
        parameterName = "params"
)
public class APICreateAutoScalingGroupMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(validValues = {AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE})
    private String scalingResourceType;

    @APIParam(numberRange = {0, Integer.MAX_VALUE})
    private Integer minResourceSize;

    @APIParam(numberRange = {0, Integer.MAX_VALUE})
    private Integer maxResourceSize;

    @APIParam(numberRange = {0, Integer.MAX_VALUE})
    private Long defaultCooldown;

    @APIParam(validValues = {
            AutoScalingConstants.REMOVAL_POLICY_OLDEST_INSTANCE,
            AutoScalingConstants.REMOVAL_POLICY_NEWEST_INSTANCE,
            AutoScalingConstants.REMOVAL_POLICY_OLDEST_SCALING_CONFIGURATION,
            AutoScalingConstants.REMOVAL_POLICY_MINIMUM_CPU_USAGE_INSTANCE,
            AutoScalingConstants.REMOVAL_POLICY_MINIMUM_MEMORY_USAGE_INSTANCE})
    private String removalPolicy;

    @APIParam(required = false)
    private boolean defaultEnable;

    public static APICreateAutoScalingGroupMsg __example__() {
        APICreateAutoScalingGroupMsg msg = new APICreateAutoScalingGroupMsg();

        msg.setName("test-group");
        msg.setDescription("just for test");
        msg.setScalingResourceType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        msg.setMaxResourceSize(10);
        msg.setMinResourceSize(2);
        msg.setDefaultCooldown(60L);
        msg.setRemovalPolicy(AutoScalingConstants.REMOVAL_POLICY_OLDEST_INSTANCE);
        return msg;
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

    public String getScalingResourceType() {
        return scalingResourceType;
    }

    public void setScalingResourceType(String scalingResourceType) {
        this.scalingResourceType = scalingResourceType;
    }

    public Integer getMinResourceSize() {
        return minResourceSize;
    }

    public void setMinResourceSize(Integer minResourceSize) {
        this.minResourceSize = minResourceSize;
    }

    public Integer getMaxResourceSize() {
        return maxResourceSize;
    }

    public void setMaxResourceSize(Integer maxResourceSize) {
        this.maxResourceSize = maxResourceSize;
    }

    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateAutoScalingGroupEvent)rsp).getInventory().getUuid() : "", AutoScalingGroupVO.class);
    }

    public String getRemovalPolicy() {
        return removalPolicy;
    }

    public void setRemovalPolicy(String removalPolicy) {
        this.removalPolicy = removalPolicy;
    }

    public boolean isDefaultEnable() {
        return defaultEnable;
    }

    public void setDefaultEnable(boolean defaultEnable) {
        this.defaultEnable = defaultEnable;
    }

    public Long getDefaultCooldown() {
        return defaultCooldown;
    }

    public void setDefaultCooldown(Long defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }
}
