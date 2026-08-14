package org.zstack.sdk.zwatch.alarm;

import org.zstack.sdk.zwatch.alarm.EventSubscriptionState;

public class EventSubscriptionInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
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

    public EventSubscriptionState state;
    public void setState(EventSubscriptionState state) {
        this.state = state;
    }
    public EventSubscriptionState getState() {
        return this.state;
    }

    public java.util.List actions;
    public void setActions(java.util.List actions) {
        this.actions = actions;
    }
    public java.util.List getActions() {
        return this.actions;
    }

    public java.util.List labels;
    public void setLabels(java.util.List labels) {
        this.labels = labels;
    }
    public java.util.List getLabels() {
        return this.labels;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.lang.String emergencyLevel;
    public void setEmergencyLevel(java.lang.String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
    public java.lang.String getEmergencyLevel() {
        return this.emergencyLevel;
    }

}
