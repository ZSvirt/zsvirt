package org.zstack.header.message;

import com.rabbitmq.client.AMQP.BasicProperties;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
 * Transferring object to make BasicProperties serializable
 */
public class MessageProperties implements Serializable {
    private String contentType;
    private String contentEncoding;
    private Map<String, Object> headers = new HashMap<String, Object>();
    private Integer deliveryMode;
    private Integer priority;
    private String correlationId;
    private String replyTo;
    private String expiration;
    private String messageId;
    private Date timestamp;
    private String type;
    private String userId;
    private String appId;
    private String clusterId;

    public static MessageProperties valueOf(BasicProperties bp) {
        MessageProperties mp = new MessageProperties();
        mp.setAppId(bp.getAppId());
        mp.setClusterId(bp.getClusterId());
        mp.setContentEncoding(bp.getContentEncoding());
        mp.setContentType(bp.getContentType());
        mp.setCorrelationId(bp.getCorrelationId());
        mp.setDeliveryMode(bp.getDeliveryMode());
        mp.setExpiration(bp.getExpiration());
        mp.setHeaders(bp.getHeaders());
        mp.setMessageId(bp.getMessageId());
        mp.setPriority(bp.getPriority());
        mp.setReplyTo(bp.getReplyTo());
        mp.setTimestamp(bp.getTimestamp());
        mp.setType(bp.getType());
        mp.setUserId(bp.getUserId());
        return mp;
    }

    public BasicProperties toBasicProperties() {
        BasicProperties.Builder builder = new BasicProperties.Builder();
        return builder.appId(appId).clusterId(clusterId).contentEncoding(contentEncoding).contentType(contentType).correlationId(correlationId)
                .deliveryMode(deliveryMode).expiration(expiration).headers(headers).messageId(messageId).priority(priority).replyTo(replyTo)
                .timestamp(timestamp).type(type).userId(userId).build();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(Integer deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
}
