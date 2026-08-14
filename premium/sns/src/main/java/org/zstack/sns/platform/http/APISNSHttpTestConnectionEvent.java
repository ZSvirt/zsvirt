package org.zstack.sns.platform.http;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"connected", "webhookResp"})
public class APISNSHttpTestConnectionEvent extends APIEvent {
    private boolean connected;
    private String webhookResp;

    public APISNSHttpTestConnectionEvent() {
    }

    public APISNSHttpTestConnectionEvent(String apiId) {
        super(apiId);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getWebhookResp() {
        return webhookResp;
    }

    public void setWebhookResp(String webhookResp) {
        this.webhookResp = webhookResp;
    }

    public static APISNSHttpTestConnectionEvent __example__() {
        APISNSHttpTestConnectionEvent event = new APISNSHttpTestConnectionEvent();
        event.setConnected(true);
        event.setWebhookResp("{'result': 'success'}");
        return event;
    }

}
