package org.zstack.autoscaling.group.activity;

/**
 * Created by lining on 2018/9/4.
 */
public enum AutoScalingGroupActivityCause {
    HealthCheck("HealthCheck"),
    RuleTakesEffect("RuleTakesEffect"),
    MaintainTheNumberOfInstances("MaintainTheNumberOfInstances"),
    ManualOperation("ManualOperation");

    private String name;

    AutoScalingGroupActivityCause(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
