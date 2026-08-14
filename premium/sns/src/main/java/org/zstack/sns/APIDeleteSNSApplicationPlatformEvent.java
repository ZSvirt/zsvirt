package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteSNSApplicationPlatformEvent extends APIEvent {
    public static APIDeleteSNSApplicationPlatformEvent __example__() {
        APIDeleteSNSApplicationPlatformEvent evt = new APIDeleteSNSApplicationPlatformEvent();
        return evt;
    }

    public APIDeleteSNSApplicationPlatformEvent() {
    }

    public APIDeleteSNSApplicationPlatformEvent(String apiId) {
        super(apiId);
    }
}
