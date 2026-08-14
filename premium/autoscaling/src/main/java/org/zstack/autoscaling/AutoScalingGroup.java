package org.zstack.autoscaling;

import org.zstack.header.message.Message;

/**
 * Created by lining on 2018/9/13.
 */
public interface AutoScalingGroup {
    void handleMessage(Message msg);
}
