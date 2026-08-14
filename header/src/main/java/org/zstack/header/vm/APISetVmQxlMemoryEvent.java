package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmQxlMemoryEvent extends APIEvent {
    public APISetVmQxlMemoryEvent() {
    }

    public APISetVmQxlMemoryEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmQxlMemoryEvent __example__() {
        APISetVmQxlMemoryEvent event = new APISetVmQxlMemoryEvent();
        return event;
    }
}
