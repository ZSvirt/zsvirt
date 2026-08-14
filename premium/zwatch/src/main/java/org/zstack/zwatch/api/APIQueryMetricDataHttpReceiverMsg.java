package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.metricpusher.MetricDataHttpReceiverInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryMetricDataHttpReceiverReply.class, inventoryClass = MetricDataHttpReceiverInventory.class)
@RestRequest(path = "/zwatch/metrics/httpreceivers", optionalPaths = {"/zwatch/metrics/httpreceivers/{uuid}"},
        responseClass = APIQueryMetricDataHttpReceiverReply.class, method = HttpMethod.GET)
public class APIQueryMetricDataHttpReceiverMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("name=CloudMonitor"));
        return ret;
    }
}
