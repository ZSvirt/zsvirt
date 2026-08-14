package org.zstack.autoscaling.group.activity.action;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;

import java.util.List;

/**
 * Created by lining on 2018/9/13.
 */
public class AutoScalingGroupRemoveInstancesActionMsg extends AutoScalingGroupActivityActionMessage implements AutoScalingGroupMessage {

    private List<String> instanceUuids;

    private int removalInstanceSize;

    private boolean ignoreInstanceSizeLimit;

    public List<String> getInstanceUuids() {
        return instanceUuids;
    }

    public void setInstanceUuids(List<String> instanceUuids) {
        this.instanceUuids = instanceUuids;
    }

    public int getRemovalInstanceSize() {
        return removalInstanceSize;
    }

    public void setRemovalInstanceSize(int removalInstanceSize) {
        this.removalInstanceSize = removalInstanceSize;
    }

    @Override
    public boolean ignoreInstanceSizeLimit() {
        return this.ignoreInstanceSizeLimit;
    }

    public void setIgnoreInstanceSizeLimit(boolean ignoreInstanceSizeLimit) {
        this.ignoreInstanceSizeLimit = ignoreInstanceSizeLimit;
    }
}
