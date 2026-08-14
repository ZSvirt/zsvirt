package org.zstack.zwatch.alarm;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@AutoDeleteTag
@PrimaryKeyJoinColumn(name="alertDataUuid", referencedColumnName = "alertDataUuid")
public class AlarmDataAckVO extends AlertDataAckVO implements ToInventory {
    @Column
    private String alarmUuid;

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}
