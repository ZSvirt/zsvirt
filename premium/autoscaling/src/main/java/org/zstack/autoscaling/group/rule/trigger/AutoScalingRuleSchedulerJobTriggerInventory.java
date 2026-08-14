package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.configuration.*;
import org.zstack.header.search.*;

import java.util.*;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/8 10:57 AM
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingRuleSchedulerJobTriggerVO.class, collectionValueOfMethod = "valueOf2")
public class AutoScalingRuleSchedulerJobTriggerInventory extends AutoScalingRuleTriggerInventory {
    private String schedulerJobUuid;

    public AutoScalingRuleSchedulerJobTriggerInventory() {
    }

    public AutoScalingRuleSchedulerJobTriggerInventory(AutoScalingRuleSchedulerJobTriggerVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setType(vo.getType().toString());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setRuleUuid(vo.getRuleUuid());
        this.setState(vo.getState().toString());
        this.setSchedulerJobUuid(vo.getSchedulerJobUuid());
    }

    public static AutoScalingRuleSchedulerJobTriggerInventory valueOf(AutoScalingRuleSchedulerJobTriggerVO vo) {
        return new AutoScalingRuleSchedulerJobTriggerInventory(vo);
    }

    public static List<AutoScalingRuleSchedulerJobTriggerInventory> valueOf2(Collection<AutoScalingRuleSchedulerJobTriggerVO> vos) {
        List<AutoScalingRuleSchedulerJobTriggerInventory> invs = new ArrayList<AutoScalingRuleSchedulerJobTriggerInventory>();
        for (AutoScalingRuleSchedulerJobTriggerVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }
}
