package org.zstack.autoscaling.group.activity.action;

/**
 * Created by lining on 2018/9/14.
 */
public interface AutoScalingCreateInstancesActionExtensionPoint {
    void beforeCreateInstances(AutoScalingGroupCreateInstancesActionMsg msg);

    void preCreateInstances(AutoScalingGroupCreateInstancesActionMsg msg);

    void afterCreateInstancesSuccess(AutoScalingGroupCreateInstancesActionMsg msg, CreateInstancesResult result);

}
