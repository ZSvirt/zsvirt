package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "labelValues")
public class APIGetPrometheusMetricLabelValueReply extends APIReply {
    private Map<String, List<String>> labelValues;

    public Map<String, List<String>> getLabelValues() {
        return labelValues;
    }

    public void setLabelValues(Map<String, List<String>> labelValues) {
        this.labelValues = labelValues;
    }

    public static APIGetPrometheusMetricLabelValueReply __example__() {
        APIGetPrometheusMetricLabelValueReply ret = new APIGetPrometheusMetricLabelValueReply();
        Map<String, List<String>> map = new HashMap<>();
        map.put("cpuNum", list("0", "1", "2", "4"));
        map.put("hostUuid", list("host_uuid_1", "host_uuid_2"));
        ret.labelValues = map;
        return ret;
    }
}