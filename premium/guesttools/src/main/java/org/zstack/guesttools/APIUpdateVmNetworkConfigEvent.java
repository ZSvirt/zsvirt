package org.zstack.guesttools;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUpdateVmNetworkConfigEvent extends APIEvent{
    public APIUpdateVmNetworkConfigEvent() {
    }

    public APIUpdateVmNetworkConfigEvent(String apiId) {
        super(apiId);
    }
    public static APIUpdateVmNetworkConfigEvent __example__() {
        APIUpdateVmNetworkConfigEvent event = new APIUpdateVmNetworkConfigEvent();

        return event;
    }
}
