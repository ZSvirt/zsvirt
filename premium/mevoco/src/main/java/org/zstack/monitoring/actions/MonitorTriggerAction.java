package org.zstack.monitoring.actions;

import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/6/11.
 */
public interface MonitorTriggerAction {
    void handleMessage(Message msg);
}
