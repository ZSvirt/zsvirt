package org.zstack.autoscaling.group.activity.action;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/12.
 */
public abstract class AutoScalingGroupActivityActionMessage extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String autoScalingGroupActivityUuid;

    private String autoScalingGroupUuid;

    public boolean ignoreInstanceSizeLimit() {
        return false;
    }

    public String getAutoScalingGroupActivityUuid() {
        return autoScalingGroupActivityUuid;
    }

    public void setAutoScalingGroupActivityUuid(String autoScalingGroupActivityUuid) {
        this.autoScalingGroupActivityUuid = autoScalingGroupActivityUuid;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
