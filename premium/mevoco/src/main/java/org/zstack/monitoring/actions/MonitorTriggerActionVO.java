package org.zstack.monitoring.actions;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;
import org.zstack.monitoring.MonitorTriggerActionRefVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xing5 on 2017/6/10.
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class MonitorTriggerActionVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    @Enumerated(EnumType.STRING)
    private MonitorTriggerActionState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "actionUuid", insertable = false, updatable = false)
    private Set<MonitorTriggerActionRefVO> triggerRefs = new HashSet<>();

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


    public MonitorTriggerActionVO() {
    }

    public MonitorTriggerActionVO(MonitorTriggerActionVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.state = other.state;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    public MonitorTriggerActionState getState() {
        return state;
    }

    public void setState(MonitorTriggerActionState state) {
        this.state = state;
    }

    public Set<MonitorTriggerActionRefVO> getTriggerRefs() {
        return triggerRefs;
    }

    public void setTriggerRefs(Set<MonitorTriggerActionRefVO> triggerRefs) {
        this.triggerRefs = triggerRefs;
    }

    @PreUpdate
    void preUpdate() {
        lastOpDate = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
