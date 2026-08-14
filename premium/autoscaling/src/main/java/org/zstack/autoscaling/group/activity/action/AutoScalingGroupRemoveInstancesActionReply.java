package org.zstack.autoscaling.group.activity.action;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2018/9/14.
 */

/**
 * All successful : success = true & result.errorCodes is empty
 * Partially successful : success = true & result.errorCodes not empty
 * All failed : success = false & (errorCode not null | result.errorCodes not empty)
 */
public class AutoScalingGroupRemoveInstancesActionReply extends MessageReply {
    private RemoveInstancesResult result;

    public RemoveInstancesResult getResult() {
        return result;
    }

    public void setResult(RemoveInstancesResult result) {
        this.result = result;
    }
}
