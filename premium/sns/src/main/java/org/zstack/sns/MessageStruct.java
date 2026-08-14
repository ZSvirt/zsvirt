package org.zstack.sns;

import java.util.Map;

public class MessageStruct {
    private Map metadata;
    private String message;

    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
