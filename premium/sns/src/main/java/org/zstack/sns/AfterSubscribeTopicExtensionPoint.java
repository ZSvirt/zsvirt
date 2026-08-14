package org.zstack.sns;

public interface AfterSubscribeTopicExtensionPoint {
    void afterSubscribeTopic(SNSTopicInventory topic, SNSApplicationEndpointInventory endpoint);
}
