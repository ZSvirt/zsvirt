package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@AutoDeleteTag
public class MonitorGroupEventSubscriptionVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String groupUuid;

    @Column
    private String eventSubscriptionUuid;

    @Column
    private String eventRuleTemplateUuid;

    @Column
    private Timestamp createDate;

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

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getEventSubscriptionUuid() {
        return eventSubscriptionUuid;
    }

    public void setEventSubscriptionUuid(String eventSubscriptionUuid) {
        this.eventSubscriptionUuid = eventSubscriptionUuid;
    }

    public String getEventRuleTemplateUuid() {
        return eventRuleTemplateUuid;
    }

    public void setEventRuleTemplateUuid(String eventRuleTemplateUuid) {
        this.eventRuleTemplateUuid = eventRuleTemplateUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
