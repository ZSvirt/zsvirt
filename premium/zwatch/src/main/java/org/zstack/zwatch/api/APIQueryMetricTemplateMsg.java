package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.metricpusher.MetricTemplateInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryMetricTemplateReply.class, inventoryClass = MetricTemplateInventory.class)
@RestRequest(path = "/zwatch/metrics/httpreceivers/templates", optionalPaths = {"/zwatch/metrics/httpreceivers/templates/{uuid}"},
        responseClass = APIQueryMetricDataHttpReceiverReply.class, method = HttpMethod.GET)
public class APIQueryMetricTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("receiverUuid=%s", uuid()));
        return ret;
    }
}
