package org.zstack.monitoring;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/3.
 */
@Entity
@Table
public class MonitorTriggerVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String expression;

    @Column
    private String recoveryExpression;

    @Column
    private int duration;

    @Column
    private String description;

    @Column
    private String contextData;

    @Column
    @Enumerated(EnumType.STRING)
    private MonitorTriggerStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private MonitorTriggerState state;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = ResourceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String targetResourceUuid;

    @Column
    private Timestamp lastStatusChangeTime;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    void preUpdate() {
        lastOpDate = null;
    }

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


    public String getContextData() {
        return contextData;
    }

    public void setContextData(String contextData) {
        this.contextData = contextData;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public Timestamp getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }

    public void setLastStatusChangeTime(Timestamp lastStatusChangeTime) {
        this.lastStatusChangeTime = lastStatusChangeTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getRecoveryExpression() {
        return recoveryExpression;
    }

    public void setRecoveryExpression(String recoveryExpression) {
        this.recoveryExpression = recoveryExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MonitorTriggerStatus getStatus() {
        return status;
    }

    public void setStatus(MonitorTriggerStatus status) {
        this.status = status;
    }

    public MonitorTriggerState getState() {
        return state;
    }

    public void setState(MonitorTriggerState state) {
        this.state = state;
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
