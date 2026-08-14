package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SmsReceiverType;

public class SNSSmsReceiverInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String phoneNumber;
    public void setPhoneNumber(java.lang.String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public java.lang.String getPhoneNumber() {
        return this.phoneNumber;
    }

    public java.lang.String endpointUuid;
    public void setEndpointUuid(java.lang.String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
    public java.lang.String getEndpointUuid() {
        return this.endpointUuid;
    }

    public SmsReceiverType type;
    public void setType(SmsReceiverType type) {
        this.type = type;
    }
    public SmsReceiverType getType() {
        return this.type;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
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
