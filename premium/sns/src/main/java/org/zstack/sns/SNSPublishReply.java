package org.zstack.sns;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class SNSPublishReply extends MessageReply {
    private List<SNSPublishError> errors;

    public List<SNSPublishError> getErrors() {
        return errors;
    }

    public void setErrors(List<SNSPublishError> errors) {
        this.errors = errors;
    }
}
