package org.zstack.autoscaling.group;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/11.
 */
public class DeleteAutoScalingGroupMsg extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String autoScalingGroupUuid;

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
