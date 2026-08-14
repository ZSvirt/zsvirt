package org.zstack.autoscaling.group.activity.action;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2018/9/14.
 */
public class AutoScalingRemoveInstancesReply extends MessageReply {
    RemoveInstancesResult result;

    public RemoveInstancesResult getResult() {
        return result;
    }

    public void setResult(RemoveInstancesResult result) {
        this.result = result;
    }
}
