package org.zstack.sdk.zwatch.alarm.activealarm.entity;

import org.zstack.sdk.zwatch.ruleengine.ComparisonOperator;
import org.zstack.sdk.zwatch.datatype.EmergencyLevel;

public class ActiveAlarmTemplateInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String alarmName;
    public void setAlarmName(java.lang.String alarmName) {
        this.alarmName = alarmName;
    }
    public java.lang.String getAlarmName() {
        return this.alarmName;
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

}
