package org.zstack.autoscaling.group.activity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by lining at 2018/9/28
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingGroupActivityVO.class, collectionValueOfMethod = "valueOf1")
public class AutoScalingGroupActivityInventory implements Serializable {
    private String uuid;

    private String name;

    private String scalingGroupUuid;

    private String activityAction;

    private String instanceUuids;

    private String scalingGroupRuleUuid;

    private String cause;

    private String description;

    private String status;

    private String activityActionResultMessage;

    private Timestamp endDate;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public AutoScalingGroupActivityInventory() {
    }

    public AutoScalingGroupActivityInventory(AutoScalingGroupActivityVO vo) {
        this.setUuid(vo.getUuid());
        this.setScalingGroupUuid(vo.getScalingGroupUuid());
        this.setStatus(vo.getStatus().toString());
        this.setActivityAction(vo.getActivityAction().toString());
        this.setInstanceUuids(vo.getInstanceUuids());
        this.setScalingGroupRuleUuid(vo.getScalingGroupRuleUuid());
        this.setCause(vo.getCause().toString());
        this.setActivityActionResultMessage(vo.getActivityActionResultMessage());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setEndDate(vo.getEndDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static AutoScalingGroupActivityInventory valueOf(AutoScalingGroupActivityVO vo) {
        return new AutoScalingGroupActivityInventory(vo);
    }

    public static List<AutoScalingGroupActivityInventory> valueOf1(Collection<AutoScalingGroupActivityVO> vos) {
        List<AutoScalingGroupActivityInventory> invs = new ArrayList<AutoScalingGroupActivityInventory>();
        for (AutoScalingGroupActivityVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public String getActivityAction() {
        return activityAction;
    }

    public void setActivityAction(String activityAction) {
        this.activityAction = activityAction;
    }

    public String getScalingGroupRuleUuid() {
        return scalingGroupRuleUuid;
    }

    public void setScalingGroupRuleUuid(String scalingGroupRuleUuid) {
        this.scalingGroupRuleUuid = scalingGroupRuleUuid;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActivityActionResultMessage() {
        return activityActionResultMessage;
    }

    public void setActivityActionResultMessage(String activityActionResultMessage) {
        this.activityActionResultMessage = activityActionResultMessage;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getInstanceUuids() { return instanceUuids; }

    public void setInstanceUuids(String instanceUuids) { this.instanceUuids = instanceUuids; }
}
