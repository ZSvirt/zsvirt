package org.zstack.autoscaling.group.activity.action;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by lining on 2018/9/13.
 */
public abstract class AutoScalingGroupRemoveInstancesMsg extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String autoScalingGroupUuid;

    private int removalInstanceSize;

    private List<String> instanceUuids;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }

    public int getRemovalInstanceSize() {
        return removalInstanceSize;
    }

    public void setRemovalInstanceSize(int removalInstanceSize) {
        this.removalInstanceSize = removalInstanceSize;
    }

    public List<String> getInstanceUuids() {
        return instanceUuids;
    }

    public void setInstanceUuids(List<String> instanceUuids) {
        this.instanceUuids = instanceUuids;
    }
}
