package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAddStorageProtocolEvent extends APIEvent {
    public APIAddStorageProtocolEvent() {
    }

    public APIAddStorageProtocolEvent(String apiId) {
        super(apiId);
    }

    public static APIAddStorageProtocolEvent __example__() {
        APIAddStorageProtocolEvent event = new APIAddStorageProtocolEvent();
        return event;
    }
}
