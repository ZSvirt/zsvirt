package org.zstack.header.scheduler;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SchedulerJobGroupVO.class, myField = "schedulerJobGroupUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = SchedulerJobVO.class, myField = "schedulerJobUuid", targetField = "uuid"),
        }
)
public class SchedulerJobGroupJobRefVO {
    @Column
    @Id
    @ForeignKey(parentEntityClass = SchedulerJobVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String schedulerJobUuid;

    @Column
    @ForeignKey(parentEntityClass = SchedulerJobGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String schedulerJobGroupUuid;

    @Column
    private Integer priority = 0;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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
