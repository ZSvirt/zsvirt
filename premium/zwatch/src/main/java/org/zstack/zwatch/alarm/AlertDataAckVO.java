package org.zstack.zwatch.alarm;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class AlertDataAckVO {

    @Id
    @Column
    private String alertDataUuid;

    @Column
    private String alertType;

    @Column
    private long ackPeriod;

    @Column
    private String resourceUuid;

    @Column
    private Timestamp ackDate;

    @Column
    private boolean resumeAlert;

    @Column
    private String operatorAccountUuid;

    public String getAlertDataUuid() {
        return alertDataUuid;
    }

    public void setAlertDataUuid(String alertDataUuid) {
        this.alertDataUuid = alertDataUuid;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public long getAckPeriod() {
        return ackPeriod;
    }

    public void setAckPeriod(long ackPeriod) {
        this.ackPeriod = ackPeriod;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public Timestamp getAckDate() {
        return ackDate;
    }

    public void setAckDate(Timestamp ackDate) {
        this.ackDate = ackDate;
    }

    public boolean isResumeAlert() {
        return resumeAlert;
    }

    public void setResumeAlert(boolean resumeAlert) {
        this.resumeAlert = resumeAlert;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }
}
