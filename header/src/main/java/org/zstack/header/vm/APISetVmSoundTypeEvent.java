package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmSoundTypeEvent extends APIEvent {
    public APISetVmSoundTypeEvent() {
    }

    public APISetVmSoundTypeEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmSoundTypeEvent __example__() {
        APISetVmSoundTypeEvent event = new APISetVmSoundTypeEvent();
        return event;
    }
}
