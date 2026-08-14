package org.zstack.zwatch.alarm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@IdClass(AlarmActionVO.CompositeId.class)
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = AlarmVO.class, myField = "alarmUuid", targetField = "uuid")
        }
)
public class AlarmActionVO {
    static class CompositeId implements Serializable {
        private String alarmUuid;
        private String actionUuid;

        public String getAlarmUuid() {
            return alarmUuid;
        }

        public void setAlarmUuid(String alarmUuid) {
            this.alarmUuid = alarmUuid;
        }

        public String getActionUuid() {
            return actionUuid;
        }

        public void setActionUuid(String actionUuid) {
            this.actionUuid = actionUuid;
        }
    }

    @Column
    @Id
    @ForeignKey(parentKey = "uuid", parentEntityClass = AlarmVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String alarmUuid;
    @Column
    private String actionType;
    @Column
    @Id
    private String actionUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
