package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteSNSApplicationEndpointEvent extends APIEvent {
    public APIDeleteSNSApplicationEndpointEvent() {
    }

    public APIDeleteSNSApplicationEndpointEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteSNSApplicationEndpointEvent __example__() {
        return new APIDeleteSNSApplicationEndpointEvent();
    }
}
