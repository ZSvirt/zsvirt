package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2018/10/9.
 */

@RestRequest(
        path = "/autoscaling/groups/rules/triggers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAutoScalingRuleTriggerEvent.class
)
public class APIDeleteAutoScalingRuleTriggerMsg extends APIDeleteMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingRuleTriggerVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APINoSee
    private String autoScalingGroupUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteAutoScalingRuleTriggerMsg __example__() {
        APIDeleteAutoScalingRuleTriggerMsg msg = new APIDeleteAutoScalingRuleTriggerMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
