package org.zstack.log.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteLogServerEvent extends APIEvent {
    public APIDeleteLogServerEvent() {
    }

    public APIDeleteLogServerEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteLogServerEvent __example__() {
        APIDeleteLogServerEvent event = new APIDeleteLogServerEvent();
        return event;
    }
}
