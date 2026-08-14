package org.zstack.header.cbt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteCbtTaskEvent extends APIEvent {
    public APIDeleteCbtTaskEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteCbtTaskEvent() {
        super(null);
    }

    public static APIDeleteCbtTaskEvent __example__() {
        APIDeleteCbtTaskEvent event = new APIDeleteCbtTaskEvent();

        return event;
    }
}
