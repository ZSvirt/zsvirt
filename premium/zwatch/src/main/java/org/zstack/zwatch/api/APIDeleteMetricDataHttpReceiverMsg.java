package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.metricpusher.MetricDataHttpReceiverVO;

@RestRequest(
        path = "/zwatch/metrics/httpreceivers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMetricDataHttpReceiverEvent.class)
public class APIDeleteMetricDataHttpReceiverMsg extends APIDeleteMessage {
    @APIParam(successIfResourceNotExisting = true, resourceType = MetricDataHttpReceiverVO.class)
    private String uuid;

    public static APIDeleteMetricDataHttpReceiverMsg __example__() {
        APIDeleteMetricDataHttpReceiverMsg ret = new APIDeleteMetricDataHttpReceiverMsg();
        ret.uuid = uuid();
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
