package org.zstack.sns.platform.wecom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.LinkedHashMap;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(fieldsTo = {"connected", "webhookResp"})
public class APISNSWeComTestConnectionEvent extends APIEvent {

    private boolean connected;
    private LinkedHashMap<String, Object> webhookResp;

    public APISNSWeComTestConnectionEvent() {
    }

    public APISNSWeComTestConnectionEvent(String apiId) {
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

    public static APISNSWeComTestConnectionEvent __example__() {
        APISNSWeComTestConnectionEvent event = new APISNSWeComTestConnectionEvent();
        event.setConnected(true);
        event.setWebhookResp(new LinkedHashMap<>());
        return event;
    }

}
