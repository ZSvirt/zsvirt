package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.AlarmMessage;
import org.zstack.zwatch.metricpusher.MetricTemplateVO;

@RestRequest(
        path = "/zwatch/metrics/httpreceivers/templates/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMetricTemplateEvent.class)
public class APIDeleteMetricTemplateMsg extends APIDeleteMessage implements AlarmMessage {
    @APIParam(successIfResourceNotExisting = true, resourceType = MetricTemplateVO.class)
    private String uuid;

    public static APIDeleteMetricTemplateMsg __example__() {
        APIDeleteMetricTemplateMsg ret = new APIDeleteMetricTemplateMsg();
        ret.uuid = uuid();
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAlarmUuid() {
        return uuid;
    }
}
