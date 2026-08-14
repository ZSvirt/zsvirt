package org.zstack.autoscaling.group;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@RestRequest(
        path = "/autoscaling/groups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAutoScalingGroupEvent.class,
        isAction = true
)
public class APIUpdateAutoScalingGroupMsg extends APIMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingGroupVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, numberRange = {0, Integer.MAX_VALUE})
    private Integer minResourceSize;

    @APIParam(required = false,numberRange = {0, Integer.MAX_VALUE})
    private Integer maxResourceSize;

    @APIParam(required = false, validValues = {
            AutoScalingConstants.REMOVAL_POLICY_OLDEST_INSTANCE,
            AutoScalingConstants.REMOVAL_POLICY_NEWEST_INSTANCE,
            AutoScalingConstants.REMOVAL_POLICY_OLDEST_SCALING_CONFIGURATION ,
            AutoScalingConstants.REMOVAL_POLICY_MINIMUM_CPU_USAGE_INSTANCE,
            AutoScalingConstants.REMOVAL_POLICY_MINIMUM_MEMORY_USAGE_INSTANCE})
    private String removalPolicy;

    public static APIUpdateAutoScalingGroupMsg __example__() {
        APIUpdateAutoScalingGroupMsg msg = new APIUpdateAutoScalingGroupMsg();

        msg.setUuid(uuid());
        msg.setName("test-group2");
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

    public String getRemovalPolicy() {
        return removalPolicy;
    }

    public void setRemovalPolicy(String removalPolicy) {
        this.removalPolicy = removalPolicy;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return uuid;
    }
}
