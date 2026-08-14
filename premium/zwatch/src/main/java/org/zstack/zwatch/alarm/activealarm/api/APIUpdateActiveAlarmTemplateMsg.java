package org.zstack.zwatch.alarm.activealarm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateVO;
import org.zstack.zwatch.ruleengine.ComparisonOperator;


@RestRequest(
        path = "/zwatch/activealarms/templates/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateActiveAlarmTemplateEvent.class
)
public class APIUpdateActiveAlarmTemplateMsg extends APIMessage {
    @APIParam(resourceType = ActiveAlarmTemplateVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String alarmName;
    @APIParam(validValues = {"GreaterThanOrEqualTo", "GreaterThan", "LessThan", "LessThanOrEqualTo"}, required = false)
    private String comparisonOperator;
    @APIParam(numberRange = {1, Integer.MAX_VALUE}, required = false)
    private Integer period;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Double threshold;
    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer repeatInterval;
    @APIParam(numberRange = {-1, Integer.MAX_VALUE}, required = false)
    private Integer repeatCount;
    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel;
    @APIParam(required = false)
    private String labels;

    public static APIUpdateActiveAlarmTemplateMsg __example__() {
        APIUpdateActiveAlarmTemplateMsg ret = new APIUpdateActiveAlarmTemplateMsg();
        ret.uuid = uuid();
        ret.alarmName = "VM-CPUAverageUsedUtilization";
        ret.comparisonOperator = ComparisonOperator.LessThanOrEqualTo.toString();
        ret.threshold = 80D;
        ret.repeatInterval = 1800;
        ret.period = 60;
        ret.repeatCount = -1;
        ret.emergencyLevel = "Emergent";
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
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

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }
}
