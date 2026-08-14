package org.zstack.sns;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.Message;

import java.util.List;

public interface SNSApplicationPlatform {
    void handleMessage(Message msg);

    void publish(SNSTopicInventory topic, MessageStruct message, ReturnValueCompletion<List<SNSPublishError>> completion);
}
