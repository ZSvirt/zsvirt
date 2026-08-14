package org.zstack.sns;

public interface SNSTopicFactory {
    SNSTopic createSNSTopic(String uuid);
}
