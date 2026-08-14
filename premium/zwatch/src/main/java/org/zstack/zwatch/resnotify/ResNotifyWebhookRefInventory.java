package org.zstack.zwatch.resnotify;

import org.zstack.header.rest.APINoSee;
import org.zstack.header.log.NoLogging;

import java.sql.Timestamp;



public class ResNotifyWebhookRefInventory {
    private String uuid;
    private String webhookUrl;
    @APINoSee
    private String secret;
    @NoLogging
    private String customHeaders;

    public static ResNotifyWebhookRefInventory valueOf(ResNotifyWebhookRefVO vo) {
        ResNotifyWebhookRefInventory inv = new ResNotifyWebhookRefInventory();
        inv.setUuid(vo.getUuid());
        inv.setWebhookUrl(vo.getWebhookUrl());
        inv.setCustomHeaders(vo.getCustomHeaders());
        return inv;
    }

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
