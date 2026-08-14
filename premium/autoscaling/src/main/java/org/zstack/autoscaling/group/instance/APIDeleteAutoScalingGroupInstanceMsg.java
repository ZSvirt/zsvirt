package org.zstack.autoscaling.group.instance;

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
        path = "/autoscaling/groups/instances/{instanceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAutoScalingGroupInstanceEvent.class
)
public class APIDeleteAutoScalingGroupInstanceMsg extends APIDeleteMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingGroupInstanceVO.class, successIfResourceNotExisting = true)
    private String instanceUuid;

    @APINoSee
    private String autoScalingGroupUuid;

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public static APIDeleteAutoScalingGroupInstanceMsg __example__() {
        APIDeleteAutoScalingGroupInstanceMsg msg = new APIDeleteAutoScalingGroupInstanceMsg();
        msg.setInstanceUuid(uuid());

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
