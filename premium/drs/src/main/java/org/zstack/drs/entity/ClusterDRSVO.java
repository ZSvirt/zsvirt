package org.zstack.drs.entity;

import org.zstack.drs.data.BalancedState;
import org.zstack.drs.data.DRSAutomationLevel;
import org.zstack.drs.data.DRSState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
public class ClusterDRSVO extends ResourceVO implements ToInventory {

    @Column
    private String name;

    @Column
    @ForeignKey(parentEntityClass = ClusterVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private DRSState state;

    @Column
    @Enumerated(EnumType.STRING)
    private BalancedState balancedState;

    @Column
    private String lastAdviceGroupUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private DRSAutomationLevel automationLevel;

    @Column
    private String thresholds;

    @Column
    private Integer thresholdDuration;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public DRSState getState() {
        return state;
    }

    public void setState(DRSState state) {
        this.state = state;
    }

    public String getThresholds() {
        return thresholds;
    }

    public void setThresholds(String thresholds) {
        this.thresholds = thresholds;
    }

    public Integer getThresholdDuration() {
        return thresholdDuration;
    }

    public void setThresholdDuration(Integer thresholdDuration) {
        this.thresholdDuration = thresholdDuration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DRSAutomationLevel getAutomationLevel() {
        return automationLevel;
    }

    public void setAutomationLevel(DRSAutomationLevel automationLevel) {
        this.automationLevel = automationLevel;
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

    public BalancedState getBalancedState() {
        return balancedState;
    }

    public void setBalancedState(BalancedState balancedState) {
        this.balancedState = balancedState;
    }

    public String getLastAdviceGroupUuid() {
        return lastAdviceGroupUuid;
    }

    public void setLastAdviceGroupUuid(String lastAdviceGroupUuid) {
        this.lastAdviceGroupUuid = lastAdviceGroupUuid;
    }
}
