package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.prometheus.PrometheusDatabaseDriver;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "metrics")
public class APIGetAllMetricMetadataReply extends APIReply {
    public static class MetricStruct {
        public String namespace;
        public String name;
        public String description;
        public List<String> labelNames;
        public String driver;

        public static MetricStruct __example__() {
            MetricStruct ret = new MetricStruct();
            ret.namespace = "ZStack/VM";
            ret.name = VmNamespace.CPUIdleUtilization.getName();
            ret.labelNames = VmNamespace.CPUIdleUtilization.getLabelNames();
            ret.description = "CPUIdleUtilization";
            ret.driver = PrometheusDatabaseDriver.class.getSimpleName();
            return ret;
        }
    }

    public static APIGetAllMetricMetadataReply __example__() {
        APIGetAllMetricMetadataReply ret = new APIGetAllMetricMetadataReply();
        ret.setMetrics(asList(MetricStruct.__example__()));
        return ret;
    }

    private List<MetricStruct> metrics;

    public List<MetricStruct> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricStruct> metrics) {
        this.metrics = metrics;
    }
}
