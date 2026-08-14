package org.zstack.sns;

import org.zstack.header.message.Message;

public interface SNSTopic {
    void handleMessage(Message msg);
}
