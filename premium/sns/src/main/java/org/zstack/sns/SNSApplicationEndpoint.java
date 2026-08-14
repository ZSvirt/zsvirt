package org.zstack.sns;

import org.zstack.header.core.Completion;
import org.zstack.header.message.Message;

public interface SNSApplicationEndpoint {
    void handleMessage(Message msg);

    void publish(MessageStruct message, Completion completion);

    SNSApplicationEndpointInventory getInventory();
}
