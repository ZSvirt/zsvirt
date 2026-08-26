package org.zstack.zwatch.resnotify;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class ResNotifyWebhookRefVO {

    @Id
    @Column
    private String uuid; // FK to ResNotifySubscriptionVO.uuid

    @Column(nullable = false)
    private String webhookUrl;

    @Column
    private String secret;

    @Column(length = 2048)
    private String customHeaders;

    // Getters and Setters

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
    }
}
