package org.zstack.header.configuration;

import org.zstack.header.message.Message;


public interface InstanceOffering {
    void handleMessage(Message msg);

    void deleteHook();
}
