package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIChangeHostPasswordEvent extends APIEvent {
    public APIChangeHostPasswordEvent() {
    }

    public APIChangeHostPasswordEvent(String apiId) {
        super(apiId);
    }

    public static APIChangeHostPasswordEvent __example__() {
        APIChangeHostPasswordEvent evt = new APIChangeHostPasswordEvent();
        return evt;
    }
}
