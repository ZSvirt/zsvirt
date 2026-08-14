package org.zstack.monitoring.prometheus;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/9/2.
 */
@RestRequest(
        path = "/prometheus/labels",
        method = HttpMethod.GET,
        responseClass = APIPrometheusQueryLabelValuesReply.class
)
@Deprecated
public class APIPrometheusQueryLabelValuesMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private List<String> labels;

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
 
    public static APIPrometheusQueryLabelValuesMsg __example__() {
        APIPrometheusQueryLabelValuesMsg msg = new APIPrometheusQueryLabelValuesMsg();
        msg.setLabels(Arrays.asList("label"));
        return msg;
    }

}
