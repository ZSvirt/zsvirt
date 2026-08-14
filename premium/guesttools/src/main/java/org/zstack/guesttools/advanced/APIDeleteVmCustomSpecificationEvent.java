package org.zstack.guesttools.advanced;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVmCustomSpecificationEvent extends APIEvent {
    public APIDeleteVmCustomSpecificationEvent() {
        super(null);
    }

    public APIDeleteVmCustomSpecificationEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVmCustomSpecificationEvent __example__() {
        return new APIDeleteVmCustomSpecificationEvent();
    }
}
