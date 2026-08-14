package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * @author: kefeng.wang
 * @date: 2018-11-17
 **/
@RestRequest(path = "/zwatch/metrics",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMetricDataEvent.class)
public class APIDeleteMetricDataMsg extends APIMessage {
    @APIParam // required
    private String namespace;

    @APIParam // required
    private String metricName;

    private List<String> labels;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public static APIDeleteMetricDataMsg __example__() {
        APIDeleteMetricDataMsg msg = new APIDeleteMetricDataMsg();
        msg.setNamespace("ZStack/Host");
        msg.setMetricName("CPUUsedUtilization");
        msg.setLabels(Collections.singletonList("HostUuid=98272eda895e41c2901f73cbd1cb24de"));
        return msg;
    }
}
