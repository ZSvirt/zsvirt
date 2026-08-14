package org.zstack.autoscaling.group.rule;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@RestRequest(
        path = "/autoscaling/rules/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAutoScalingRuleEvent.class
)
public class APIDeleteAutoScalingRuleMsg extends APIDeleteMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingRuleVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APINoSee
    private String autoScalingGroupUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteAutoScalingRuleMsg __example__() {
        APIDeleteAutoScalingRuleMsg msg = new APIDeleteAutoScalingRuleMsg();
        msg.setUuid(uuid());

        return msg;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }
}
