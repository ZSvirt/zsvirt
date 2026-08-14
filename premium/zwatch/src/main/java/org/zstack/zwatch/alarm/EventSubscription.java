package org.zstack.zwatch.alarm;

import org.zstack.header.message.Message;

public interface EventSubscription {
    void handleMessage(Message msg);
}
