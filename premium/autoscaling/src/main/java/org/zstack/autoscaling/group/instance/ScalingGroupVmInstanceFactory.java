package org.zstack.autoscaling.group.instance;

import org.zstack.autoscaling.group.ScalingResourceType;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupCreateInstancesMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupCreateVmInstancesMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupRemoveInstancesMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupRemoveVmInstancesMsg;
import org.zstack.autoscaling.group.instance.vm.GetRemoveTargetVmInstanceListMsg;
import org.zstack.autoscaling.group.rule.RemovalPolicy;

import java.util.List;

/**
 * Created by lining on 2018/9/13.
 */
public class ScalingGroupVmInstanceFactory implements ScalingGroupInstanceFactory{
    @Override
    public ScalingResourceType getType() {
        return ScalingResourceType.VmInstance;
    }

    @Override
    public AutoScalingGroupCreateInstancesMsg getAutoScalingCreateInstanceMsg(String autoScalingGroup, int addingInstanceSize) {
        AutoScalingGroupCreateVmInstancesMsg msg = new AutoScalingGroupCreateVmInstancesMsg();
        msg.setAddingInstanceSize(addingInstanceSize);
        msg.setAutoScalingGroupUuid(autoScalingGroup);

        return msg;
    }

    @Override
    public AutoScalingGroupRemoveInstancesMsg getAutoScalingRemoveInstanceMsg(String autoScalingGroup, List<String> instanceUuids) {
        AutoScalingGroupRemoveVmInstancesMsg msg = new AutoScalingGroupRemoveVmInstancesMsg();
        msg.setRemovalInstanceSize(instanceUuids.size());
        msg.setAutoScalingGroupUuid(autoScalingGroup);
        msg.setInstanceUuids(instanceUuids);
        return msg;
    }

    @Override
    public GetRemoveTargetInstanceListMsg getRemoveTargetInstanceListMsg(String autoScalingGroup, int size, RemovalPolicy policy) {
        GetRemoveTargetVmInstanceListMsg msg = new GetRemoveTargetVmInstanceListMsg();
        msg.setAutoScalingGroupUuid(autoScalingGroup);
        msg.setPolicy(policy);
        msg.setSize(size);

        return msg;
    }

}
