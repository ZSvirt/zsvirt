package org.zstack.autoscaling.group.instance;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.autoscaling.group.rule.RemovalPolicy;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/17.
 */
public class GetRemoveTargetInstanceListMsg extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String autoScalingGroupUuid;

    private RemovalPolicy policy;

    private int size;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }

    public RemovalPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(RemovalPolicy policy) {
        this.policy = policy;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
