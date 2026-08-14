package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmCleanTrafficEvent extends APIEvent {
    public APISetVmCleanTrafficEvent(String apiId) {
        super(apiId);
    }

    public APISetVmCleanTrafficEvent() {
        super();
    }


    public static APISetVmCleanTrafficEvent __example__() {
        return new APISetVmCleanTrafficEvent();
    }
}
