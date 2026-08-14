package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(path = "/zwatch/metrics/label-values", method = HttpMethod.GET, responseClass = APIGetMetricLabelValueReply.class)
public class APIGetMetricLabelValueMsg extends APISyncCallMessage {
    @APIParam
    private String namespace;
    @APIParam
    private String metricName;
    @APIParam(numberRange = {0L, 253402271999L}, required = false)
    private Long startTime;
    @APIParam(numberRange = {0L, 253402271999L}, required = false)
    private Long endTime;
    @APIParam(nonempty = true)
    private List<String> labelNames;
    @APIParam(required = false)
    private List<String> filterLabels;

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

    public List<String> getLabelNames() {
        return labelNames;
    }

    public void setLabelNames(List<String> labelNames) {
        this.labelNames = labelNames;
    }

    public List<String> getFilterLabels() {
        return filterLabels;
    }

    public void setFilterLabels(List<String> filterLabels) {
        this.filterLabels = filterLabels;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public static APIGetMetricLabelValueMsg __example__() {
        APIGetMetricLabelValueMsg msg = new APIGetMetricLabelValueMsg();
        msg.setNamespace("ZStack/VM");
        msg.setMetricName(VmNamespace.CPUIdleUtilization.getName());
        msg.labelNames = asList("CPUNum");
        msg.filterLabels = asList("VMUuid=" + uuid(VmInstanceVO.class));
        msg.startTime = DocUtils.dateInSeconds();
        msg.endTime = DocUtils.dateInSecondsAndAddSeconds(60);
        return msg;
    }
}
