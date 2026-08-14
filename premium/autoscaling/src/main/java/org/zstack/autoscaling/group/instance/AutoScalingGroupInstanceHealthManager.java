package org.zstack.autoscaling.group.instance;

/**
 * Created by lining on 2018/9/30.
 */
public interface AutoScalingGroupInstanceHealthManager {
    void installInstanceStateEventListener();

    void handleUnhealthyInstances();

    //TODO
    //void installInstanceStateChecker();
}
