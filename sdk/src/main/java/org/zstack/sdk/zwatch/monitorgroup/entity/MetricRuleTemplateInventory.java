package org.zstack.sdk.zwatch.monitorgroup.entity;

import org.zstack.sdk.zwatch.ruleengine.ComparisonOperator;
import org.zstack.sdk.zwatch.datatype.EmergencyLevel;

public class MetricRuleTemplateInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String monitorTemplateUuid;
    public void setMonitorTemplateUuid(java.lang.String monitorTemplateUuid) {
        this.monitorTemplateUuid = monitorTemplateUuid;
    }
    public java.lang.String getMonitorTemplateUuid() {
        return this.monitorTemplateUuid;
    }

    public ComparisonOperator comparisonOperator;
    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }
    public ComparisonOperator getComparisonOperator() {
        return this.comparisonOperator;
    }

    public int period;
    public void setPeriod(int period) {
        this.period = period;
    }
    public int getPeriod() {
        return this.period;
    }

    public int repeatInterval;
    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }
    public int getRepeatInterval() {
        return this.repeatInterval;
    }

    public int repeatCount;
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }
    public int getRepeatCount() {
        return this.repeatCount;
    }

    public java.lang.String namespace;
    public void setNamespace(java.lang.String namespace) {
        this.namespace = namespace;
    }
    public java.lang.String getNamespace() {
        return this.namespace;
    }

    public java.lang.String metricName;
    public void setMetricName(java.lang.String metricName) {
        this.metricName = metricName;
    }
    public java.lang.String getMetricName() {
        return this.metricName;
    }

    public double threshold;
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    public double getThreshold() {
        return this.threshold;
    }

    public EmergencyLevel emergencyLevel;
    public void setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
    public EmergencyLevel getEmergencyLevel() {
        return this.emergencyLevel;
    }

    public java.lang.String labels;
    public void setLabels(java.lang.String labels) {
        this.labels = labels;
    }
    public java.lang.String getLabels() {
        return this.labels;
    }

    public boolean enableRecovery;
    public void setEnableRecovery(boolean enableRecovery) {
        this.enableRecovery = enableRecovery;
    }
    public boolean getEnableRecovery() {
        return this.enableRecovery;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

}
