package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Timestamp;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@AutoDeleteTag
public class MonitorGroupAlarmVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String groupUuid;

    @Column
    private String alarmUuid;

    @Column
    private String metricRuleTemplateUuid;

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

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getMetricRuleTemplateUuid() {
        return metricRuleTemplateUuid;
    }

    public void setMetricRuleTemplateUuid(String metricRuleTemplateUuid) {
        this.metricRuleTemplateUuid = metricRuleTemplateUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
