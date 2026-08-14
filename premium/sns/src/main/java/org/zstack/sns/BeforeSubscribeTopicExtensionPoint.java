package org.zstack.sns;

public interface BeforeSubscribeTopicExtensionPoint {
    void beforeSubscribeTopic(SNSTopicInventory topic, SNSApplicationEndpointInventory endpoint);
}
