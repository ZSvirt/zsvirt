package org.zstack.autoscaling.group.instance;

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
@Inventory(mappingVOClass = AutoScalingGroupInstanceVO.class, collectionValueOfMethod = "valueOf1")
public class AutoScalingGroupInstanceInventory implements Serializable {
    private String uuid;

    private String instanceUuid;

    private String scalingGroupUuid;

    private String templateUuid;

    private String scalingGroupActivityUuid;

    private String status;

    private String healthStatus;

    private String description;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String protectionStrategy;

    public AutoScalingGroupInstanceInventory() {
    }

    public AutoScalingGroupInstanceInventory(AutoScalingGroupInstanceVO vo) {
        this.setUuid(vo.getUuid());
        this.setScalingGroupUuid(vo.getScalingGroupUuid());
        this.setTemplateUuid(vo.getTemplateUuid());
        this.setScalingGroupActivityUuid(vo.getScalingGroupActivityUuid());
        this.setStatus(vo.getStatus().toString());
        this.setHealthStatus(vo.getHealthStatus().toString());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setInstanceUuid(vo.getInstanceUuid());
        this.setProtectionStrategy(vo.getProtectionStrategy());
    }

    public static AutoScalingGroupInstanceInventory valueOf(AutoScalingGroupInstanceVO vo) {
        return new AutoScalingGroupInstanceInventory(vo);
    }

    public static List<AutoScalingGroupInstanceInventory> valueOf1(Collection<AutoScalingGroupInstanceVO> vos) {
        List<AutoScalingGroupInstanceInventory> invs = new ArrayList<AutoScalingGroupInstanceInventory>();
        for (AutoScalingGroupInstanceVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getScalingGroupActivityUuid() {
        return scalingGroupActivityUuid;
    }

    public void setScalingGroupActivityUuid(String scalingGroupActivityUuid) {
        this.scalingGroupActivityUuid = scalingGroupActivityUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
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

    public String getProtectionStrategy() {
        return protectionStrategy;
    }

    public void setProtectionStrategy(String protectionStrategy) {
        this.protectionStrategy = protectionStrategy;
    }
}
