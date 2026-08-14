package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachDataVolumeFromHostEvent extends APIEvent {
    public APIDetachDataVolumeFromHostEvent() {
    }

    public APIDetachDataVolumeFromHostEvent(String apiId) {
        super(apiId);
    }
    public static APIDetachDataVolumeFromHostEvent __example__() {
        APIDetachDataVolumeFromHostEvent event = new APIDetachDataVolumeFromHostEvent();
        event.setSuccess(true);
        return event;
    }
}
