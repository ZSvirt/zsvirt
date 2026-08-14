package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmBootModeEvent extends APIEvent {
    public APISetVmBootModeEvent() {
    }

    public APISetVmBootModeEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmBootModeEvent __example__() {
        APISetVmBootModeEvent event = new APISetVmBootModeEvent();
        return event;
    }

}
