package org.zstack.monitoring;

import org.zstack.header.vo.ForeignKey;
import org.zstack.monitoring.actions.MonitorTriggerActionVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/12.
 */
@Entity
@Table
@IdClass(MonitorTriggerActionRefVO.Id.class)
public class MonitorTriggerActionRefVO {
    static class Id implements Serializable {
        private String actionUuid;
        private String triggerUuid;

        public String getActionUuid() {
            return actionUuid;
        }

        public void setActionUuid(String actionUuid) {
            this.actionUuid = actionUuid;
        }

        public String getTriggerUuid() {
            return triggerUuid;
        }

        public void setTriggerUuid(String triggerUuid) {
            this.triggerUuid = triggerUuid;
        }
    }

    @javax.persistence.Id
    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = MonitorTriggerVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String triggerUuid;
    @javax.persistence.Id
    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = MonitorTriggerActionVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String actionUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
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
