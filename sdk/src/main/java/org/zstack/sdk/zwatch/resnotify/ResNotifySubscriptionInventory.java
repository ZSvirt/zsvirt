package org.zstack.sdk.zwatch.resnotify;

import org.zstack.sdk.zwatch.resnotify.ResNotifyType;
import org.zstack.sdk.zwatch.resnotify.ResNotifySubscriptionState;
import org.zstack.sdk.zwatch.resnotify.ResNotifyWebhookRefInventory;

public class ResNotifySubscriptionInventory  {

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

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String resourceTypes;
    public void setResourceTypes(java.lang.String resourceTypes) {
        this.resourceTypes = resourceTypes;
    }
    public java.lang.String getResourceTypes() {
        return this.resourceTypes;
    }

    public java.lang.String eventTypes;
    public void setEventTypes(java.lang.String eventTypes) {
        this.eventTypes = eventTypes;
    }
    public java.lang.String getEventTypes() {
        return this.eventTypes;
    }

    public ResNotifyType type;
    public void setType(ResNotifyType type) {
        this.type = type;
    }
    public ResNotifyType getType() {
        return this.type;
    }

    public ResNotifySubscriptionState state;
    public void setState(ResNotifySubscriptionState state) {
        this.state = state;
    }
    public ResNotifySubscriptionState getState() {
        return this.state;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
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

    public ResNotifyWebhookRefInventory webhookRef;
    public void setWebhookRef(ResNotifyWebhookRefInventory webhookRef) {
        this.webhookRef = webhookRef;
    }
    public ResNotifyWebhookRefInventory getWebhookRef() {
        return this.webhookRef;
    }

}
