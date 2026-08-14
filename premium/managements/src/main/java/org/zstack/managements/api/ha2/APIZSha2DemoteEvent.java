package org.zstack.managements.api.ha2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = "all")
public class APIZSha2DemoteEvent extends APIEvent {
    public APIZSha2DemoteEvent() {
    }

    public APIZSha2DemoteEvent(String apiId) {
        super(apiId);
    }

    public static APIZSha2DemoteEvent __example__() {
        return new APIZSha2DemoteEvent();
    }
}
