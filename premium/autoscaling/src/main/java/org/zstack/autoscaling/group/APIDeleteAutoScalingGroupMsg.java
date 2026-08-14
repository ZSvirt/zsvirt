package org.zstack.autoscaling.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@RestRequest(
        path = "/autoscaling/groups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAutoScalingGroupEvent.class
)
public class APIDeleteAutoScalingGroupMsg extends APIDeleteMessage implements AutoScalingGroupMessage{
    @APIParam(resourceType = AutoScalingGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteAutoScalingGroupMsg __example__() {
        APIDeleteAutoScalingGroupMsg msg = new APIDeleteAutoScalingGroupMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return uuid;
    }
}
