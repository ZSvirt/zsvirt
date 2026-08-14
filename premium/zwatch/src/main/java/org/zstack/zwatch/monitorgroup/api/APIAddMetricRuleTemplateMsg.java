package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.List;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/monitortemplates/{monitorTemplateUuid}/metricrules",
        method = HttpMethod.POST,
        responseClass = APIAddMetricRuleTemplateEvent.class,
        parameterName = "params"
)
public class APIAddMetricRuleTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = MonitorTemplateVO.class)
    private String monitorTemplateUuid;

    @APIParam(validValues = {"GreaterThanOrEqualTo", "GreaterThan", "LessThan", "LessThanOrEqualTo"})
    private String comparisonOperator;

    @APIParam(numberRange = {1, Integer.MAX_VALUE}, required = false)
    private Integer period;

    @APIParam
    private String namespace;

    @APIParam
    private String metricName;

    @APIParam(numberRange = {0, Long.MAX_VALUE})
    private Double threshold;

    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Integer repeatInterval;

    @APIParam(required = false)
    private List<Label> labels;

    @APIParam(numberRange = {-1, Integer.MAX_VALUE}, required = false)
    private Integer repeatCount = -1;

    @APIParam(required = false)
    private Boolean enableRecovery = false;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel = "Important";

    public static APIAddMetricRuleTemplateMsg __example__() {
        APIAddMetricRuleTemplateMsg msg = new APIAddMetricRuleTemplateMsg();
        msg.setNamespace("ZStack/VM");
        msg.setName("vm-cpu");
        msg.setPeriod(60);
        msg.setThreshold(80d);
        msg.setEnableRecovery(true);
        msg.setMetricName(VmNamespace.CPUUsedUtilization.getName());
        msg.setMonitorTemplateUuid(uuid());
        msg.setComparisonOperator( ComparisonOperator.GreaterThan.toString());
        msg.setEmergencyLevel(EmergencyLevel.Normal.name());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddMetricRuleTemplateEvent)rsp).getInventory().getMonitorTemplateUuid() : "", MonitorTemplateVO.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMonitorTemplateUuid() {
        return monitorTemplateUuid;
    }

    public void setMonitorTemplateUuid(String monitorTemplateUuid) {
        this.monitorTemplateUuid = monitorTemplateUuid;
    }

    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
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

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Boolean getEnableRecovery() {
        return enableRecovery;
    }

    public void setEnableRecovery(Boolean enableRecovery) {
        this.enableRecovery = enableRecovery;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
}
