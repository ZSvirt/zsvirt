package org.zstack.zwatch.alarm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.List;

/**
 * Created by yaoning.li on 2020/10/15.
 */
public class CreateAlarmMsg extends NeedReplyMessage {
    private String uuid;

    private String name;

    private String description;

    private ComparisonOperator comparisonOperator;

    private Integer period;

    private String namespace;

    private String metricName;

    private Double threshold;

    private Integer repeatInterval;

    private List<Label> labels;

    private List<APICreateAlarmMsg.ActionParam> actions;

    private Integer repeatCount;

    private String type;

    private boolean enableRecovery;

    private String emergencyLevel;

    private String accountUuid;

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

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
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

    public List<APICreateAlarmMsg.ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<APICreateAlarmMsg.ActionParam> actions) {
        this.actions = actions;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getEnableRecovery() {
        return enableRecovery;
    }

    public void setEnableRecovery(boolean enableRecovery) {
        this.enableRecovery = enableRecovery;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
