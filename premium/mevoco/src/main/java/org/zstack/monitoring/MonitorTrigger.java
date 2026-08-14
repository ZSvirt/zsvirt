package org.zstack.monitoring;

import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/6/10.
 */
public interface MonitorTrigger {
    void handleMessage(Message msg);
}
