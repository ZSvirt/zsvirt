package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateVO;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.List;

@RestRequest(
        path = "/zwatch/monitortemplates/metricrules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateMetricRuleTemplateEvent.class,
        isAction = true
)
public class APIUpdateMetricRuleTemplateMsg extends APIMessage {
    @APIParam(resourceType = MetricRuleTemplateVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(required = false, validValues = {"GreaterThanOrEqualTo", "GreaterThan", "LessThan", "LessThanOrEqualTo"})
    private String comparisonOperator;

    @APIParam(required = false, numberRange = {1, Integer.MAX_VALUE})
    private Integer period;

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
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

    public static APIUpdateMetricRuleTemplateMsg __example__() {
        APIUpdateMetricRuleTemplateMsg msg = new APIUpdateMetricRuleTemplateMsg();
        msg.setUuid(uuid());
        msg.setComparisonOperator(ComparisonOperator.GreaterThanOrEqualTo.name());
        msg.setPeriod(10);
        msg.setThreshold(90d);
        msg.setRepeatCount(1);
        msg.setEnableRecovery(true);
        msg.setEmergencyLevel(EmergencyLevel.Important.name());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
