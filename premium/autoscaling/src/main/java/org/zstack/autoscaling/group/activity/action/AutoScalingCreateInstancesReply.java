package org.zstack.autoscaling.group.activity.action;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2018/9/14.
 */
public class AutoScalingCreateInstancesReply extends MessageReply {
    CreateInstancesResult result;

    public CreateInstancesResult getResult() {
        return result;
    }

    public void setResult(CreateInstancesResult result) {
        this.result = result;
    }
}
