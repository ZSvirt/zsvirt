package org.zstack.header.protocol;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveVRouterNetworksFromOspfAreaEvent extends APIEvent {
    public APIRemoveVRouterNetworksFromOspfAreaEvent() {
        super(null);
    }

    public APIRemoveVRouterNetworksFromOspfAreaEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveVRouterNetworksFromOspfAreaEvent __example__() {
        APIRemoveVRouterNetworksFromOspfAreaEvent event = new APIRemoveVRouterNetworksFromOspfAreaEvent();
        event.setSuccess(true);
        return event;
    }

}

