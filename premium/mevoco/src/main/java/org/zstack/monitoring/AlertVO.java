package org.zstack.monitoring;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/15.
 */
@Entity
@Table
public class AlertVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String triggerUuid;
    @Column
    private String targetResourceUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private MonitorTriggerStatus triggerStatus;
    @Column
    private String content;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    public MonitorTriggerStatus getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(MonitorTriggerStatus triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
