package org.zstack.autoscaling.group.activity;

/**
 * Created by lining on 2018/9/14.
 */
public enum AutoScalingGroupActivityAction {
    AddingNewInstance("AddingNewInstance"),
    RemovalInstance("RemovalInstance"),
    UpgradeInstance("UpgradeInstance");

    private String name;

    AutoScalingGroupActivityAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
