package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.ValueCondition;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/metrics",
        method = HttpMethod.GET,
        responseClass = APIGetMetricDataReply.class
)
public class APIGetMetricDataMsg extends APISyncCallMessage {
    @APIParam
    private String namespace;
    @APIParam
    private String metricName;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long startTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long endTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long offsetAheadOfCurrentTime;
    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer period;
    private List<String> labels;
    private List<String> valueConditions;
    private List<String> functions;

    public static APIGetMetricDataMsg __example__() {
        APIGetMetricDataMsg msg = new APIGetMetricDataMsg();
        msg.setNamespace("ZStack/VM");
        msg.setMetricName(VmNamespace.CPUIdleUtilization.getName());
        msg.setStartTime(DocUtils.dateInSeconds());
        msg.setEndTime(DocUtils.dateInSecondsAndAddSeconds(60));
        msg.setPeriod(30);
        msg.setLabels(asList(VmNamespace.LabelNames.VMUuid.toString() + '=' + uuid(VmInstanceVO.class),
                VmNamespace.LabelNames.CPUNum.toString() + '=' + 4));
        msg.setValueConditions(asList(ValueCondition.Key.value.toString() + ">5",
                ValueCondition.Key.value.toString() + "<10"));
        return msg;
    }

    @APINoSee
    private List<Function> functionList;
    @APINoSee
    private List<Label> labelList = new ArrayList<>();
    @APINoSee
    private List<ValueCondition> valueConditionList = new ArrayList<>();

    public Long getOffsetAheadOfCurrentTime() {
        return offsetAheadOfCurrentTime;
    }

    public void setOffsetAheadOfCurrentTime(Long offsetAheadOfCurrentTime) {
        this.offsetAheadOfCurrentTime = offsetAheadOfCurrentTime;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public void setFunctions(List<String> functions) {
        this.functions = functions;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

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

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public List<Label> getLabelList() {
        return labelList;
    }

    public void setLabelList(List<Label> labelList) {
        this.labelList = labelList;
    }

    public List<Function> getFunctionList() {
        return functionList;
    }

    public void setFunctionList(List<Function> functionList) {
        this.functionList = functionList;
    }

    public List<String> getValueConditions() {
        return valueConditions;
    }

    public void setValueConditions(List<String> valueConditions) {
        this.valueConditions = valueConditions;
    }

    public List<ValueCondition> getValueConditionList() {
        return valueConditionList;
    }

    public void setValueConditionList(List<ValueCondition> valueConditionList) {
        this.valueConditionList = valueConditionList;
    }
}
