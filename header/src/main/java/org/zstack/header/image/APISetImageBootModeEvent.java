package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetImageBootModeEvent extends APIEvent {
    public APISetImageBootModeEvent() {
    }

    public APISetImageBootModeEvent(String apiId) {
        super(apiId);
    }

    public static APISetImageBootModeEvent __example__() {
        APISetImageBootModeEvent evt = new APISetImageBootModeEvent();
        return evt;
    }
}
