package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse()
public class APIFstrimVmEvent extends APIEvent {
    public APIFstrimVmEvent() {
    }

    public APIFstrimVmEvent(String apiId) {
        super(apiId);
    }

    public static APIFstrimVmEvent __example__() {
        return new APIFstrimVmEvent();
    }
}
