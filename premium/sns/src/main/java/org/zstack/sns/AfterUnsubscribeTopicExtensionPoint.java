package org.zstack.sns;

public interface AfterUnsubscribeTopicExtensionPoint {
    void afterUnsubscribeTopic(SNSTopicInventory topic, SNSApplicationEndpointInventory endpoint);
}
