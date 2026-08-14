package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by lining at 2018/9/22
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingRuleTriggerVO.class, collectionValueOfMethod = "valueOf1")
public class AutoScalingRuleTriggerInventory implements Serializable {
    private String name;

    private String uuid;

    @TypeField
    private String type;

    private String ruleUuid;

    private String description;

    private String state;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public AutoScalingRuleTriggerInventory() {
    }

    public AutoScalingRuleTriggerInventory(AutoScalingRuleTriggerVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setType(vo.getType().toString());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setRuleUuid(vo.getRuleUuid());
        this.setState(vo.getState().toString());
    }

    public static AutoScalingRuleTriggerInventory valueOf(AutoScalingRuleTriggerVO vo) {
        return new AutoScalingRuleTriggerInventory(vo);
    }

    public static List<AutoScalingRuleTriggerInventory> valueOf1(Collection<AutoScalingRuleTriggerVO> vos) {
        List<AutoScalingRuleTriggerInventory> invs = new ArrayList<AutoScalingRuleTriggerInventory>();
        for (AutoScalingRuleTriggerVO vo : vos) {
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

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
