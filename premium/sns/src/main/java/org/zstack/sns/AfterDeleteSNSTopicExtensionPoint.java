package org.zstack.sns;

public interface AfterDeleteSNSTopicExtensionPoint {
    void afterDeleteSNSTopic(SNSTopicInventory topic);
}
