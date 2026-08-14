package org.zstack.sdk.sns.platform.microsoftteams;



public class SNSMicrosoftTeamsTestConnectionResult {
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
