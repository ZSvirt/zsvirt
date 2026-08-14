package org.zstack.imagereplicator;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteImageReplicationGroupEvent extends APIEvent {

    public APIDeleteImageReplicationGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteImageReplicationGroupEvent() {
        super(null);
    }

    public static APIDeleteImageReplicationGroupEvent __example__() {
        APIDeleteImageReplicationGroupEvent event = new APIDeleteImageReplicationGroupEvent();
        return event;
    }
}
