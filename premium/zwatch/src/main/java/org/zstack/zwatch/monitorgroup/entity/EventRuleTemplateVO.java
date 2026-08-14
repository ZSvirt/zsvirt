package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@AutoDeleteTag
public class EventRuleTemplateVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String name;

    @Column
    private String monitorTemplateUuid;

    @Column
    private String namespace;

    @Column
    private String eventName;

    @Column
    @Enumerated(EnumType.STRING)
    private EmergencyLevel emergencyLevel;

    @Column
    private String labels;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

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

    public EmergencyLevel getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getMonitorTemplateUuid() {
        return monitorTemplateUuid;
    }

    public void setMonitorTemplateUuid(String monitorTemplateUuid) {
        this.monitorTemplateUuid = monitorTemplateUuid;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
