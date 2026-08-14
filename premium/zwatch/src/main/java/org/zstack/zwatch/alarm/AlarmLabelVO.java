package org.zstack.zwatch.alarm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = AlarmVO.class, myField = "alarmUuid", targetField = "uuid")
        }
)
public class AlarmLabelVO extends LabelAO {
    @Column
    @ForeignKey(parentEntityClass = AlarmVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String alarmUuid;

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}
