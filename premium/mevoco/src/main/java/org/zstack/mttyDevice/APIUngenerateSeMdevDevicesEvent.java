package org.zstack.mttyDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUngenerateSeMdevDevicesEvent extends APIEvent {
    public APIUngenerateSeMdevDevicesEvent() {
    }

    public APIUngenerateSeMdevDevicesEvent(String apiId) {
        super(apiId);
    }

    public static APIUngenerateSeMdevDevicesEvent __example__() {
        return new APIUngenerateSeMdevDevicesEvent();
    }
}
