package org.zstack.header.tpm.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveTpmEvent extends APIEvent {
    public APIRemoveTpmEvent(String apiId) {
        super(apiId);
    }

    public APIRemoveTpmEvent() {
        super(null);
    }

    public static APIRemoveTpmEvent __example__() {
        return new APIRemoveTpmEvent();
    }
}
