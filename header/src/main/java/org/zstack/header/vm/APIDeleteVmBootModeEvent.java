package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVmBootModeEvent extends APIEvent {
    public APIDeleteVmBootModeEvent() {
    }

    public APIDeleteVmBootModeEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVmBootModeEvent __example__() {
        APIDeleteVmBootModeEvent event = new APIDeleteVmBootModeEvent();

        return event;
    }
}
