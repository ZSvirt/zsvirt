package org.zstack.guesttools;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachGuestToolsIsoFromVmEvent extends APIEvent {
    public APIDetachGuestToolsIsoFromVmEvent() {
    }

    public APIDetachGuestToolsIsoFromVmEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachGuestToolsIsoFromVmEvent __example__() {
        return new APIDetachGuestToolsIsoFromVmEvent();
    }
}
