package org.zstack.sns;

public interface BeforeUnsubscribeTopicExtensionPoint {
    void beforeUnsubscribeTopic(SNSTopicInventory topic, SNSApplicationEndpointInventory endpoint);
}
