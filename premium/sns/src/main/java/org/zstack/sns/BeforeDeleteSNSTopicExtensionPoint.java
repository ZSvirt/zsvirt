package org.zstack.sns;

public interface BeforeDeleteSNSTopicExtensionPoint {
    void beforeDeleteSNSTopic(SNSTopicInventory topic);
}
