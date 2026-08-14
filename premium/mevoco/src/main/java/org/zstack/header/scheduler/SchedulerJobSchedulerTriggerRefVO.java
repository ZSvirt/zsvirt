package org.zstack.header.scheduler;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SchedulerJobVO.class, myField = "schedulerJobUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = SchedulerTriggerVO.class, myField = "schedulerTriggerUuid", targetField = "uuid"),
        }
)
public class SchedulerJobSchedulerTriggerRefVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = SchedulerJobVO.class)
    private String schedulerJobUuid;

    @Column
    @ForeignKey(parentEntityClass = SchedulerTriggerVO.class)
    private String schedulerTriggerUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
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
