package org.zstack.sns.platform.email;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteEmailAddressOfSNSEmailEndpointEvent extends APIEvent {
    public APIDeleteEmailAddressOfSNSEmailEndpointEvent() {
    }

    public APIDeleteEmailAddressOfSNSEmailEndpointEvent(String apiId) {
        super(apiId);
    }
}
