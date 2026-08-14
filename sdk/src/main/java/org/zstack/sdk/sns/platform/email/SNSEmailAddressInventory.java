package org.zstack.sdk.sns.platform.email;



public class SNSEmailAddressInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String emailAddress;
    public void setEmailAddress(java.lang.String emailAddress) {
        this.emailAddress = emailAddress;
    }
    public java.lang.String getEmailAddress() {
        return this.emailAddress;
    }

    public java.lang.String endpointUuid;
    public void setEndpointUuid(java.lang.String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
    public java.lang.String getEndpointUuid() {
        return this.endpointUuid;
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
