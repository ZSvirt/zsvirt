package org.zstack.sdk.sns.platform.http;



public class SNSHttpTestConnectionResult {
    public boolean connected;
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public boolean getConnected() {
        return this.connected;
    }

    public java.lang.String webhookResp;
    public void setWebhookResp(java.lang.String webhookResp) {
        this.webhookResp = webhookResp;
    }
    public java.lang.String getWebhookResp() {
        return this.webhookResp;
    }

}
