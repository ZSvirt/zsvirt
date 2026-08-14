package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.List;

@RestRequest(path = "/zwatch/alarms/{uuid}/actions", method = HttpMethod.PUT, responseClass = APIUpdateAlarmEvent.class, isAction = true)
public class APIUpdateAlarmMsg extends APIMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2047, required = false)
    private String description;
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
    @APIParam(required = false)
    private Boolean enableRecovery;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel;

    @APIParam(required = false)
    private List<APICreateAlarmMsg.ActionParam> actions;

    public static APIUpdateAlarmMsg __example__() {
        APIUpdateAlarmMsg ret = new APIUpdateAlarmMsg();
        ret.uuid = uuid();
        ret.name = "VM CPU Alarm";
        ret.enableRecovery = true;
        ret.comparisonOperator = ComparisonOperator.LessThanOrEqualTo.toString();
        ret.period = 60;
        return ret;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean isEnableRecovery() {
        return enableRecovery;
    }

    public void setEnableRecovery(Boolean enableRecovery) {
        this.enableRecovery = enableRecovery;
    }

    @Override
    public String getAlarmUuid() {
        return uuid;
    }

    public Boolean getEnableRecovery() {
        return enableRecovery;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public List<APICreateAlarmMsg.ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<APICreateAlarmMsg.ActionParam> actions) {
        this.actions = actions;
    }
}
