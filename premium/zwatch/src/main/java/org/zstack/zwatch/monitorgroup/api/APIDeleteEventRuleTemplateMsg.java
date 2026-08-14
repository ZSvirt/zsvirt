package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateVO;

@RestRequest(
        path = "/zwatch/monitortemplates/evenrules/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteEventRuleTemplateEvent.class
)
public class APIDeleteEventRuleTemplateMsg extends APIDeleteMessage {
    @APIParam(resourceType = EventRuleTemplateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteEventRuleTemplateMsg __example__() {
        APIDeleteEventRuleTemplateMsg msg = new APIDeleteEventRuleTemplateMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
