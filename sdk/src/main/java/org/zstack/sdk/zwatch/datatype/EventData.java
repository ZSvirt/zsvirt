package org.zstack.sdk.zwatch.datatype;

import org.zstack.sdk.zwatch.datatype.EmergencyLevel;

public class EventData  {

    public java.lang.String namespace;
    public void setNamespace(java.lang.String namespace) {
        this.namespace = namespace;
    }
    public java.lang.String getNamespace() {
        return this.namespace;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.util.Map labels;
    public void setLabels(java.util.Map labels) {
        this.labels = labels;
    }
    public java.util.Map getLabels() {
        return this.labels;
    }

    public EmergencyLevel emergencyLevel;
    public void setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
    public EmergencyLevel getEmergencyLevel() {
        return this.emergencyLevel;
    }

    public java.lang.String resourceId;
    public void setResourceId(java.lang.String resourceId) {
        this.resourceId = resourceId;
    }
    public java.lang.String getResourceId() {
        return this.resourceId;
    }

    public java.lang.String resourceName;
    public void setResourceName(java.lang.String resourceName) {
        this.resourceName = resourceName;
    }
    public java.lang.String getResourceName() {
        return this.resourceName;
    }

    public java.lang.String error;
    public void setError(java.lang.String error) {
        this.error = error;
    }
    public java.lang.String getError() {
        return this.error;
    }

    public long time;
    public void setTime(long time) {
        this.time = time;
    }
    public long getTime() {
        return this.time;
    }

    public java.lang.String dataUuid;
    public void setDataUuid(java.lang.String dataUuid) {
        this.dataUuid = dataUuid;
    }
    public java.lang.String getDataUuid() {
        return this.dataUuid;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String subscriptionUuid;
    public void setSubscriptionUuid(java.lang.String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }
    public java.lang.String getSubscriptionUuid() {
        return this.subscriptionUuid;
    }

    public java.lang.String readStatus;
    public void setReadStatus(java.lang.String readStatus) {
        this.readStatus = readStatus;
    }
    public java.lang.String getReadStatus() {
        return this.readStatus;
    }

}
