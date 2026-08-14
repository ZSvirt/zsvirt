package org.zstack.sns.platform.email;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.LinkedHashMap;

@RestResponse(fieldsTo = {"connected", "webhookResp"})
public class APISNSEmailTestConnectionEvent extends APIEvent {
    private boolean connected;
    private LinkedHashMap<String, Object> webhookResp;

    public APISNSEmailTestConnectionEvent() {
    }

    public APISNSEmailTestConnectionEvent(String apiId) {
        super(apiId);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public LinkedHashMap<String, Object> getWebhookResp() {
        return webhookResp;
    }

    public void setWebhookResp(LinkedHashMap<String, Object> webhookResp) {
        this.webhookResp = webhookResp;
    }

    public static APISNSEmailTestConnectionEvent __example__() {
        APISNSEmailTestConnectionEvent event = new APISNSEmailTestConnectionEvent();
        event.setConnected(true);
        event.setWebhookResp(new LinkedHashMap<>());
        return event;
    }
}
