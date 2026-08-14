package org.zstack.sdk;

import org.zstack.sdk.AccessKeyState;

public class AccessKeyInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    @Deprecated
    public java.lang.String userUuid;
    public void setUserUuid(java.lang.String userUuid) {
        this.userUuid = userUuid;
    }
    public java.lang.String getUserUuid() {
        return this.userUuid;
    }

    public java.lang.String AccessKeyID;
    public void setAccessKeyID(java.lang.String AccessKeyID) {
        this.AccessKeyID = AccessKeyID;
    }
    public java.lang.String getAccessKeyID() {
        return this.AccessKeyID;
    }

    public java.lang.String AccessKeySecret;
    public void setAccessKeySecret(java.lang.String AccessKeySecret) {
        this.AccessKeySecret = AccessKeySecret;
    }
    public java.lang.String getAccessKeySecret() {
        return this.AccessKeySecret;
    }

    public AccessKeyState state;
    public void setState(AccessKeyState state) {
        this.state = state;
    }
    public AccessKeyState getState() {
        return this.state;
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
