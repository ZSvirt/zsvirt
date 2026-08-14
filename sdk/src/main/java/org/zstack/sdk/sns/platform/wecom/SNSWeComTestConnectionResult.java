package org.zstack.sdk.sns.platform.wecom;



public class SNSWeComTestConnectionResult {
    public boolean connected;
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public boolean getConnected() {
        return this.connected;
    }

    public java.util.LinkedHashMap webhookResp;
    public void setWebhookResp(java.util.LinkedHashMap webhookResp) {
        this.webhookResp = webhookResp;
    }
    public java.util.LinkedHashMap getWebhookResp() {
        return this.webhookResp;
    }

}
