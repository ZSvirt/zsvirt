package org.zstack.sns;

import org.zstack.header.core.Completion;
import org.zstack.header.message.Message;

public interface SNSApplicationSystemEndpoint {
    void publish(MessageStruct message, Completion completion);

    String getEndpointType();
}
