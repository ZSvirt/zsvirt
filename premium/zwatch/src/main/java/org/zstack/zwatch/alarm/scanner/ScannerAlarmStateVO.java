package org.zstack.zwatch.alarm.scanner;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.sql.Timestamp;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"scannerName", "alarmType", "alarmKey"})
})
public class ScannerAlarmStateVO {
    @Id
    @Column
    private String uuid;

    @Column(nullable = false)
    private String scannerName;

    @Column(nullable = false)
    private String alarmType;

    @Column(nullable = false)
    private String alarmKey;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private Timestamp lastUpdateTime;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmKey() {
        return alarmKey;
    }

    public void setAlarmKey(String alarmKey) {
        this.alarmKey = alarmKey;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
