package org.zstack.sdk.logserver;

import org.zstack.sdk.logserver.LogCategory;
import org.zstack.sdk.logserver.LogType;
import org.zstack.sdk.logserver.LogLevel;
import org.zstack.sdk.logserver.LogServerState;

public class LogServerInventory  {

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

    public LogCategory category;
    public void setCategory(LogCategory category) {
        this.category = category;
    }
    public LogCategory getCategory() {
        return this.category;
    }

    public LogType type;
    public void setType(LogType type) {
        this.type = type;
    }
    public LogType getType() {
        return this.type;
    }

    public LogLevel level;
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    public LogLevel getLevel() {
        return this.level;
    }

    public LogServerState state;
    public void setState(LogServerState state) {
        this.state = state;
    }
    public LogServerState getState() {
        return this.state;
    }

    public java.lang.String configuration;
    public void setConfiguration(java.lang.String configuration) {
        this.configuration = configuration;
    }
    public java.lang.String getConfiguration() {
        return this.configuration;
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

}
