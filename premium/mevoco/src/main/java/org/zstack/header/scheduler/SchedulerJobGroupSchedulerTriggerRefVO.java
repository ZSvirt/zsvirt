package org.zstack.header.scheduler;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@IdClass(SchedulerJobGroupSchedulerTriggerRefVO.CompositeId.class)
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SchedulerJobGroupVO.class, myField = "schedulerJobGroupUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = SchedulerTriggerVO.class, myField = "schedulerTriggerUuid", targetField = "uuid"),
        }
)
public class SchedulerJobGroupSchedulerTriggerRefVO {
    static class CompositeId implements Serializable {
        private String schedulerJobGroupUuid;
        private String schedulerTriggerUuid;

        public String getSchedulerJobGroupUuid() {
            return schedulerJobGroupUuid;
        }

        public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
            this.schedulerJobGroupUuid = schedulerJobGroupUuid;
        }

        public String getSchedulerTriggerUuid() {
            return schedulerTriggerUuid;
        }

        public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
            this.schedulerTriggerUuid = schedulerTriggerUuid;
        }
    }

    @Column
    @Id
    @ForeignKey(parentEntityClass = SchedulerJobGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String schedulerJobGroupUuid;

    @Column
    @Id
    @ForeignKey(parentEntityClass = SchedulerTriggerVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String schedulerTriggerUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
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
