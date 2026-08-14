package org.zstack.autoscaling.group.rule;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Create by lining at 2019/8/19
 */
@RestRequest(
        path = "/autoscaling/rules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIExecuteAutoScalingRuleEvent.class,
        isAction = true
)
public class APIExecuteAutoScalingRuleMsg extends APIMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingRuleVO.class)
    private String uuid;

    @APINoSee
    private String autoScalingGroupUuid;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public static APIExecuteAutoScalingRuleMsg __example__() {
        APIExecuteAutoScalingRuleMsg msg = new APIExecuteAutoScalingRuleMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
