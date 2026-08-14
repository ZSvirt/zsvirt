package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@RestResponse(allTo = "labels")
public class APIGetMetricLabelValueReply extends APIReply {
    private List<Map> labels;

    public List<Map> getLabels() {
        return labels;
    }

    public void setLabels(List<Map> labels) {
        this.labels = labels;
    }

    public static APIGetMetricLabelValueReply __example__() {
        APIGetMetricLabelValueReply ret = new APIGetMetricLabelValueReply();
        Map m = new HashMap();
        m.put(VmNamespace.LabelNames.CPUNum.toString(), "1");
        m.put(VmNamespace.LabelNames.VMUuid.toString(), "e47f7145f4cd4fca8e2856038ecdf3e1");
        ret.labels = asList(m);
        return ret;
    }
}
