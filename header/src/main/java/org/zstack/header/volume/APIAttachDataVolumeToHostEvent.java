package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachDataVolumeToHostEvent extends APIEvent {
    public APIAttachDataVolumeToHostEvent() {
    }

    public APIAttachDataVolumeToHostEvent(String apiId) {
        super(apiId);
    }
    public static APIAttachDataVolumeToHostEvent __example__() {
        APIAttachDataVolumeToHostEvent event = new APIAttachDataVolumeToHostEvent();
        event.setSuccess(true);
        return event;
    }
}
