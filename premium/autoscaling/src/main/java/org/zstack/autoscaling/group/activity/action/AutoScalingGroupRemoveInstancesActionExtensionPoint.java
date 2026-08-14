package org.zstack.autoscaling.group.activity.action;

import java.util.List;

/**
 * Created by lining on 2018/9/14.
 */
public interface AutoScalingGroupRemoveInstancesActionExtensionPoint {
    void beforeRemoveInstances(String scalingGroupUuid, List<String> instanceUuids);

    void preRemoveInstances(String scalingGroupUuid, List<String> instanceUuids);

    void afterRemoveInstancesSuccess(String scalingGroupUuid, RemoveInstancesResult result);
}
