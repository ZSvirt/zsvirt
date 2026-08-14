package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by weiwang at 2019-04-08
 */
@RestResponse
public class APIIdentifyHostEvent extends APIEvent {
    public APIIdentifyHostEvent() {
    }

    public APIIdentifyHostEvent(String apiId) {
        super(apiId);
    }

    public static APIIdentifyHostEvent __example__() {
        APIIdentifyHostEvent evt = new APIIdentifyHostEvent();
        return evt;
    }
}
