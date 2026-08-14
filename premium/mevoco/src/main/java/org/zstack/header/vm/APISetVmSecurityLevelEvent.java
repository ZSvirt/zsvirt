package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmSecurityLevelEvent extends APIEvent {
    public APISetVmSecurityLevelEvent() {
    }

    public APISetVmSecurityLevelEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmSecurityLevelEvent __example__() {
        APISetVmSecurityLevelEvent event = new APISetVmSecurityLevelEvent();
        return event;
    }
}
