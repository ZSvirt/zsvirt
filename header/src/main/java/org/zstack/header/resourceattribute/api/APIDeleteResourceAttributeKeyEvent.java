package org.zstack.header.resourceattribute.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteResourceAttributeKeyEvent extends APIEvent {
    public APIDeleteResourceAttributeKeyEvent() {
    }

    public APIDeleteResourceAttributeKeyEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteResourceAttributeKeyEvent __example__() {
        return new APIDeleteResourceAttributeKeyEvent();
    }
}
