package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateVO;

@RestRequest(
        path = "/zwatch/monitortemplates/metricrules/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMetricRuleTemplateEvent.class
)
public class APIDeleteMetricRuleTemplateMsg extends APIDeleteMessage {
    @APIParam(resourceType = MetricRuleTemplateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteMetricRuleTemplateMsg __example__() {
        APIDeleteMetricRuleTemplateMsg msg = new APIDeleteMetricRuleTemplateMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
