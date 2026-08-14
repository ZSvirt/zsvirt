package org.zstack.sdk.zwatch.monitorgroup.entity;

import org.zstack.sdk.zwatch.datatype.EmergencyLevel;

public class EventRuleTemplateInventory  {

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

    public java.lang.String namespace;
    public void setNamespace(java.lang.String namespace) {
        this.namespace = namespace;
    }
    public java.lang.String getNamespace() {
        return this.namespace;
    }

    public java.lang.String eventName;
    public void setEventName(java.lang.String eventName) {
        this.eventName = eventName;
    }
    public java.lang.String getEventName() {
        return this.eventName;
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

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

}
