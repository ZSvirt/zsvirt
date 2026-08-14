package org.zstack.sns;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.NoJsonSchema;

import java.util.Map;

public class SNSPublishMsg extends NeedReplyMessage implements SNSTopicMessage {
    private String topicUuid;
    // key = ApplicationPlatformType, value = metadata defined by each application platform
    private String defaultMessage;
    @NoJsonSchema
    private Map<String, Object> metadata;
    // key = ApplicationPlatformType, value = message
    @NoJsonSchema
    private Map<String, String> message;

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getTopicUuid() {
        return topicUuid;
    }

    public void setTopicUuid(String topicUuid) {
        this.topicUuid = topicUuid;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> getMessage() {
        return message;
    }

    public void setMessage(Map<String, String> message) {
        this.message = message;
    }
}
