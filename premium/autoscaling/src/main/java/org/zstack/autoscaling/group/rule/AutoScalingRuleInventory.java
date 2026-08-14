package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.tag.SystemTagVO;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by weiwang at 2018/8/15
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingRuleVO.class, collectionValueOfMethod = "valueOf1")
public class AutoScalingRuleInventory implements Serializable {
    @TypeField
    private String type;

    private String description;

    private Long cooldown;

    private AutoScalingRuleState state;

    private AutoScalingRuleStatus status;

    private List<String> systemTags;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String name;

    private String uuid;

    private String scalingGroupUuid;

    private List<AutoScalingRuleTriggerInventory> ruleTriggers;

    public AutoScalingRuleInventory() {
    }

    public AutoScalingRuleInventory(AutoScalingRuleVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setType(vo.getType().toString());
        this.setDescription(vo.getDescription());
        this.setCooldown(vo.getCooldown());
        this.setState(vo.getState());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setSystemTags(new ArrayList<>());
        if (vo.getSystemTags() != null && !vo.getSystemTags().isEmpty()) {
            for (SystemTagVO tagVO : vo.getSystemTags()) {
                this.getSystemTags().add(tagVO.getTag());
            }
        }
        this.setScalingGroupUuid(vo.getScalingGroupUuid());
        this.setRuleTriggers(AutoScalingRuleTriggerInventory.valueOf1(vo.getTriggers()));
    }

    public static AutoScalingRuleInventory valueOf(AutoScalingRuleVO vo) {
        return new AutoScalingRuleInventory(vo);
    }

    public static List<AutoScalingRuleInventory> valueOf1(Collection<AutoScalingRuleVO> vos) {
        List<AutoScalingRuleInventory> invs = new ArrayList<AutoScalingRuleInventory>();
        for (AutoScalingRuleVO vo : vos) {
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

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCooldown() {
        return cooldown;
    }

    public void setCooldown(Long cooldown) {
        this.cooldown = cooldown;
    }

    public AutoScalingRuleState getState() {
        return state;
    }

    public void setState(AutoScalingRuleState state) {
        this.state = state;
    }

    public AutoScalingRuleStatus getStatus() {
        return status;
    }

    public void setStatus(AutoScalingRuleStatus status) {
        this.status = status;
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

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public List<AutoScalingRuleTriggerInventory> getRuleTriggers() {
        return ruleTriggers;
    }

    public void setRuleTriggers(List<AutoScalingRuleTriggerInventory> ruleTriggers) {
        this.ruleTriggers = ruleTriggers;
    }
}
