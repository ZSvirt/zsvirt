package org.zstack.header.vm;

import org.zstack.header.message.Message;

public interface VmInstance {
    void handleMessage(Message msg);
}
