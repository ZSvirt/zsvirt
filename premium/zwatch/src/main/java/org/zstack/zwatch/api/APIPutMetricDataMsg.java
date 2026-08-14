package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.MetricDatum;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(path = "/zwatch/metrics",
        method = HttpMethod.POST,
        responseClass = APIPutMetricDataEvent.class,
        parameterName = "params")
public class APIPutMetricDataMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String namespace;
    @APIParam(nonempty = true)
    private List<MetricDatum> data;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<MetricDatum> getData() {
        return data;
    }

    public void setData(List<MetricDatum> data) {
        this.data = data;
    }

    public static APIPutMetricDataMsg __example__() {
        APIPutMetricDataMsg ret = new APIPutMetricDataMsg();
        ret.namespace = "MyNameSpace";
        ret.data = asList(MetricDatum.__example__());
        return ret;
    }
}
