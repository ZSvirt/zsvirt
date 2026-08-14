package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetImageSecurityLevelEvent extends APIEvent {
    public APISetImageSecurityLevelEvent() {
    }

    public APISetImageSecurityLevelEvent(String apiId) {
        super(apiId);
    }

    public static APISetImageSecurityLevelEvent __example__() {
        APISetImageSecurityLevelEvent event = new APISetImageSecurityLevelEvent();
        return event;
    }
}
