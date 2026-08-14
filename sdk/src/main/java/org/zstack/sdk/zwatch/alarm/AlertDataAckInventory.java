package org.zstack.sdk.zwatch.alarm;



public class AlertDataAckInventory  {

    public java.lang.String alertDataUuid;
    public void setAlertDataUuid(java.lang.String alertDataUuid) {
        this.alertDataUuid = alertDataUuid;
    }
    public java.lang.String getAlertDataUuid() {
        return this.alertDataUuid;
    }

    public java.lang.String alertType;
    public void setAlertType(java.lang.String alertType) {
        this.alertType = alertType;
    }
    public java.lang.String getAlertType() {
        return this.alertType;
    }

    public java.lang.Long ackPeriod;
    public void setAckPeriod(java.lang.Long ackPeriod) {
        this.ackPeriod = ackPeriod;
    }
    public java.lang.Long getAckPeriod() {
        return this.ackPeriod;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.sql.Timestamp ackDate;
    public void setAckDate(java.sql.Timestamp ackDate) {
        this.ackDate = ackDate;
    }
    public java.sql.Timestamp getAckDate() {
        return this.ackDate;
    }

    public boolean resumeAlert;
    public void setResumeAlert(boolean resumeAlert) {
        this.resumeAlert = resumeAlert;
    }
    public boolean getResumeAlert() {
        return this.resumeAlert;
    }

    public java.lang.String operatorAccountUuid;
    public void setOperatorAccountUuid(java.lang.String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }
    public java.lang.String getOperatorAccountUuid() {
        return this.operatorAccountUuid;
    }

}
