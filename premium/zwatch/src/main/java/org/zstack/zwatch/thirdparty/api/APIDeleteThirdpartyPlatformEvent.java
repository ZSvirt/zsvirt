package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteThirdpartyPlatformEvent extends APIEvent {
    public APIDeleteThirdpartyPlatformEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteThirdpartyPlatformEvent() {
        super(null);
    }

    public static APIDeleteThirdpartyPlatformEvent __example__() {
        APIDeleteThirdpartyPlatformEvent evt = new APIDeleteThirdpartyPlatformEvent();
        evt.setSuccess(true);

        return evt;
    }
}
