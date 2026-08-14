package org.zstack.sdk.zwatch.alarm;



public class AlarmActionInventory  {

    public java.lang.String alarmUuid;
    public void setAlarmUuid(java.lang.String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
    public java.lang.String getAlarmUuid() {
        return this.alarmUuid;
    }

    public java.lang.String actionType;
    public void setActionType(java.lang.String actionType) {
        this.actionType = actionType;
    }
    public java.lang.String getActionType() {
        return this.actionType;
    }

    public java.lang.String actionUuid;
    public void setActionUuid(java.lang.String actionUuid) {
        this.actionUuid = actionUuid;
    }
    public java.lang.String getActionUuid() {
        return this.actionUuid;
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
