package org.zstack.autoscaling.group.instance;

import org.zstack.autoscaling.group.ScalingResourceType;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupCreateInstancesMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupRemoveInstancesMsg;
import org.zstack.autoscaling.group.rule.RemovalPolicy;

import java.util.List;

/**
 * Created by lining on 2018/9/13.
 */
public interface ScalingGroupInstanceFactory {
    ScalingResourceType getType();

    AutoScalingGroupCreateInstancesMsg getAutoScalingCreateInstanceMsg(String autoScalingGroup, int addingInstanceSize);

    AutoScalingGroupRemoveInstancesMsg getAutoScalingRemoveInstanceMsg(String autoScalingGroup, List<String> instanceUuids);

    GetRemoveTargetInstanceListMsg getRemoveTargetInstanceListMsg(String autoScalingGroup, int size, RemovalPolicy policy);
}
