package org.zstack.sns.platform.feishu;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.LinkedHashMap;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(fieldsTo = {"connected", "webhookResp"})
public class APISNSFeiShuTestConnectionEvent extends APIEvent {

    private boolean connected;
    private LinkedHashMap<String, Object> webhookResp;

    public APISNSFeiShuTestConnectionEvent() {
    }

    public APISNSFeiShuTestConnectionEvent(String apiId) {
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

    public static APISNSFeiShuTestConnectionEvent __example__() {
        APISNSFeiShuTestConnectionEvent event = new APISNSFeiShuTestConnectionEvent();
        event.setConnected(true);
        event.setWebhookResp(new LinkedHashMap<>());
        return event;
    }

}
