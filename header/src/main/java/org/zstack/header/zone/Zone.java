package org.zstack.header.zone;

import org.zstack.header.message.Message;

public interface Zone {
    void handleMessage(Message msg);
}
