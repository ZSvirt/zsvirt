package org.zstack.autoscaling.group;


public interface AutoScalingGroupStateChangedExtensionPoint {

    void afterToggleAutoScalingGroupState(String autoScalingGroupUuid, String newStateEvent);

}
