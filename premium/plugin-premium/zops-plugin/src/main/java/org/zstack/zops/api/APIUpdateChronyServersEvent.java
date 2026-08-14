package org.zstack.zops.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUpdateChronyServersEvent extends APIEvent {
    public APIUpdateChronyServersEvent() {}
    public APIUpdateChronyServersEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateChronyServersEvent __example__() {
        APIUpdateChronyServersEvent event = new APIUpdateChronyServersEvent();
        event.setSuccess(true);
        return event;
    }
}
