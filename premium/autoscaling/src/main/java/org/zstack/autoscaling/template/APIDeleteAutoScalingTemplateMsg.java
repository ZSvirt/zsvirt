package org.zstack.autoscaling.template;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@RestRequest(
        path = "/autoscaling/template/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAutoScalingTemplateEvent.class
)
public class APIDeleteAutoScalingTemplateMsg extends APIDeleteMessage {
    @APIParam(resourceType = AutoScalingTemplateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteAutoScalingTemplateMsg __example__() {
        APIDeleteAutoScalingTemplateMsg msg = new APIDeleteAutoScalingTemplateMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
