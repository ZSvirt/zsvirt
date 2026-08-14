package org.zstack.zwatch.alarm.sns;

import java.util.Map;

public class SNSTopicMessage {
    private String message;
    private Map metadata;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }
}
