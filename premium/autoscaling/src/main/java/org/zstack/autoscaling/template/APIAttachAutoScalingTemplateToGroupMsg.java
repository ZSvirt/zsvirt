package org.zstack.autoscaling.template;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@RestRequest(
        path = "/autoscaling/template/{uuid}/groups/{groupUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachAutoScalingTemplateToGroupEvent.class,
        parameterName = "params"
)
public class APIAttachAutoScalingTemplateToGroupMsg extends APIMessage {
    @APIParam(resourceType = AutoScalingTemplateVO.class)
    private String uuid;

    @APIParam(resourceType = AutoScalingGroupVO.class)
    private String groupUuid;

    public static APIAttachAutoScalingTemplateToGroupMsg __example__() {
        APIAttachAutoScalingTemplateToGroupMsg msg = new APIAttachAutoScalingTemplateToGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
