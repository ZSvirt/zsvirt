package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by lining at 2018/9/12
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingRuleAlarmTriggerVO.class, collectionValueOfMethod = "valueOf2")
public class AutoScalingRuleAlarmTriggerInventory extends AutoScalingRuleTriggerInventory {

    private String alarmUuid;

    public AutoScalingRuleAlarmTriggerInventory() {
    }

    public AutoScalingRuleAlarmTriggerInventory(AutoScalingRuleAlarmTriggerVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setType(vo.getType().toString());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setRuleUuid(vo.getRuleUuid());
        this.setState(vo.getState().toString());
        this.setAlarmUuid(vo.getAlarmUuid());
    }

    public static AutoScalingRuleAlarmTriggerInventory valueOf(AutoScalingRuleAlarmTriggerVO vo) {
        return new AutoScalingRuleAlarmTriggerInventory(vo);
    }

    public static List<AutoScalingRuleAlarmTriggerInventory> valueOf2(Collection<AutoScalingRuleAlarmTriggerVO> vos) {
        List<AutoScalingRuleAlarmTriggerInventory> invs = new ArrayList<AutoScalingRuleAlarmTriggerInventory>();
        for (AutoScalingRuleAlarmTriggerVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}
