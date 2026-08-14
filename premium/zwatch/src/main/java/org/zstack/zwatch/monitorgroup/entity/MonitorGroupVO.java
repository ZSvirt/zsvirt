package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
public class MonitorGroupVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String name;

    @Column
    private MonitorGroupState state;

    @Column
    private String actions;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "groupUuid")
    private List<MonitorGroupTemplateRefVO> monitorGroupTemplateRefs = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MonitorGroupState getState() {
        return state;
    }

    public void setState(MonitorGroupState state) {
        this.state = state;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public List<MonitorGroupTemplateRefVO> getMonitorGroupTemplateRefs() {
        return monitorGroupTemplateRefs;
    }

    public void setMonitorGroupTemplateRefs(List<MonitorGroupTemplateRefVO> monitorGroupTemplateRefs) {
        this.monitorGroupTemplateRefs = monitorGroupTemplateRefs;
    }
}
