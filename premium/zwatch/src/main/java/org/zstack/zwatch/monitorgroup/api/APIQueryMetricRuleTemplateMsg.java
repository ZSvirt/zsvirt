package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryMetricRuleTemplateReply.class, inventoryClass = MetricRuleTemplateInventory.class)
@RestRequest(
        path = "/zwatch/monitortemplates/metricrules",
        optionalPaths = {"/zwatch/monitortemplates/metricrules/{uuid}"},
        responseClass = APIQueryMetricRuleTemplateReply.class,
        method = HttpMethod.GET)
public class APIQueryMetricRuleTemplateMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("uuid=d4904ace98f834e7bf3485376742133f"));
        return ret;
    }
}
