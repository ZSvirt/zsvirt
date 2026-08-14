package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SNSApplicationPlatformInventory;

public class SNSApplicationEndpointInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

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

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String platformUuid;
    public void setPlatformUuid(java.lang.String platformUuid) {
        this.platformUuid = platformUuid;
    }
    public java.lang.String getPlatformUuid() {
        return this.platformUuid;
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

    public java.lang.String connectionStatus;
    public void setConnectionStatus(java.lang.String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
    public java.lang.String getConnectionStatus() {
        return this.connectionStatus;
    }

    public SNSApplicationPlatformInventory platform;
    public void setPlatform(SNSApplicationPlatformInventory platform) {
        this.platform = platform;
    }
    public SNSApplicationPlatformInventory getPlatform() {
        return this.platform;
    }

}
