package org.zstack.header.cbt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUnexportNbdVolumesEvent extends APIEvent {
    public APIUnexportNbdVolumesEvent(String apiId) {
        super(apiId);
    }

    public APIUnexportNbdVolumesEvent() {
        super(null);
    }

    public static APIUnexportNbdVolumesEvent __example__() {
        APIUnexportNbdVolumesEvent event = new APIUnexportNbdVolumesEvent();

        return event;
    }
}
