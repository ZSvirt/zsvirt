package org.zstack.autoscaling.group.activity.action;

/**
 * Created by lining on 2018/9/13.
 */
public class AutoScalingGroupCreateInstancesActionMsg extends AutoScalingGroupActivityActionMessage {
    private int addingInstanceSize;

    public int getAddingInstanceSize() {
        return addingInstanceSize;
    }

    public void setAddingInstanceSize(int addingInstanceSize) {
        this.addingInstanceSize = addingInstanceSize;
    }
}
