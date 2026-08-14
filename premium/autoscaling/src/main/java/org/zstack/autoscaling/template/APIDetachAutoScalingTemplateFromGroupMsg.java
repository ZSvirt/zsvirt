package org.zstack.autoscaling.template;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/16
 */
@RestRequest(
        path = "/autoscaling/template/{templateUuid}/groups/{groupUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachAutoScalingTemplateFromGroupEvent.class
)
public class APIDetachAutoScalingTemplateFromGroupMsg extends APIMessage implements AutoScalingGroupMessage {
    @APIParam(resourceType = AutoScalingTemplateVO.class)
    private String templateUuid;

    @APIParam(resourceType = AutoScalingGroupVO.class)
    private String groupUuid;

    public static APIDetachAutoScalingTemplateFromGroupMsg __example__() {
        APIDetachAutoScalingTemplateFromGroupMsg msg = new APIDetachAutoScalingTemplateFromGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setTemplateUuid(uuid());
        return msg;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return groupUuid;
    }
}
