package org.zstack.zwatch.alarm.sns;

import org.zstack.header.message.Message;

public interface SNSTextTemplate {
    void handleMessage(Message msg);
}
