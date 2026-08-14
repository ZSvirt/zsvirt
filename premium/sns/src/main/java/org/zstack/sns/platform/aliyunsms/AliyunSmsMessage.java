package org.zstack.sns.platform.aliyunsms;

/**
 * Created by Qi Le on 2019-08-01
 */
public class AliyunSmsMessage {
    private String message;
    private AliyunSmsMessageMetadata metadata;

    public AliyunSmsMessage() {
    }

    public AliyunSmsMessage(String message, AliyunSmsMessageMetadata metadata) {
        this.message = message;
        this.metadata = metadata;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AliyunSmsMessageMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AliyunSmsMessageMetadata metadata) {
        this.metadata = metadata;
    }
}
