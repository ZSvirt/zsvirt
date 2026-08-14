package org.zstack.autoscaling.group.rule;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupBase;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.AutoScalingGroupVO_;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityAction;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityCause;
import org.zstack.autoscaling.group.activity.CreateAutoScalingGroupActivityMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupCreateInstancesActionMsg;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO_;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

/**
 * Created by lining on 2018/9/21.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AutoScalingAddingNewInstanceRuleFactory implements AutoScalingRuleFactory {

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public AutoScalingRuleType getType() {
        return AutoScalingRuleType.AddingNewInstanceRule;
    }

    @Override
    public CreateAutoScalingGroupActivityMsg makeAutoScalingGroupActivity(String ruleUuid) {
        AddingNewInstanceRuleVO rule = dbf.findByUuid(ruleUuid, AddingNewInstanceRuleVO.class);
        String groupUuid = rule.getScalingGroupUuid();
        long scalingGroupInstanceCount = Q.New(AutoScalingGroupInstanceVO.class)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, groupUuid)
                .count();

        CreateAutoScalingGroupActivityMsg msg = new CreateAutoScalingGroupActivityMsg();
        String activityUuid = Platform.getUuid();
        msg.setActivityUuid(activityUuid);
        msg.setName("");
        msg.setScalingGroupUuid(groupUuid);
        msg.setScalingGroupRuleUuid(rule.getUuid());
        msg.setDescription("");
        msg.setActivityAction(AutoScalingGroupActivityAction.AddingNewInstance.toString());
        msg.setCause(AutoScalingGroupActivityCause.RuleTakesEffect.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, AutoScalingConstants.SERVICE_ID, groupUuid);

        AutoScalingGroupCreateInstancesActionMsg actionMsg = new AutoScalingGroupCreateInstancesActionMsg();
        msg.setActionMessage(actionMsg);
        actionMsg.setAddingInstanceSize(AutoScalingRuleAdjustmentUtils.getValue(rule.getAdjustmentType(), rule.getAdjustmentValue(), (int)scalingGroupInstanceCount));
        actionMsg.setAutoScalingGroupUuid(groupUuid);
        actionMsg.setAutoScalingGroupActivityUuid(activityUuid);
        bus.makeTargetServiceIdByResourceUuid(actionMsg, AutoScalingConstants.SERVICE_ID, groupUuid);

        return msg;
    }

    @Override
    public boolean skipAutoScalingGroupActivity(String ruleUuid) {
        String groupUuid = Q.New(AddingNewInstanceRuleVO.class)
                .select(AddingNewInstanceRuleVO_.scalingGroupUuid)
                .eq(AddingNewInstanceRuleVO_.uuid, ruleUuid)
                .findValue();
        long scalingGroupInstanceCount = Q.New(AutoScalingGroupInstanceVO.class)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, groupUuid)
                .count();

        int maxResourceSize = Q.New(AutoScalingGroupVO.class)
                .eq(AutoScalingGroupVO_.uuid, groupUuid)
                .select(AutoScalingGroupVO_.maxResourceSize)
                .findValue();

        if (scalingGroupInstanceCount >= maxResourceSize) {
            return true;
        }

        return false;
    }
}
