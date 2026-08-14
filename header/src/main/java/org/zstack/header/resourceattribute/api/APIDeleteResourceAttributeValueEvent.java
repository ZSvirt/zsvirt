package org.zstack.header.resourceattribute.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteResourceAttributeValueEvent extends APIEvent {
    public APIDeleteResourceAttributeValueEvent() {
    }

    public APIDeleteResourceAttributeValueEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteResourceAttributeValueEvent __example__() {
        return new APIDeleteResourceAttributeValueEvent();
    }
}
