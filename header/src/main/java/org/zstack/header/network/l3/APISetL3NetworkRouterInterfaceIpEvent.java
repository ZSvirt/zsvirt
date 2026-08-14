package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APISetL3NetworkRouterInterfaceIpEvent extends APIEvent {
    public APISetL3NetworkRouterInterfaceIpEvent() {
    }

    public APISetL3NetworkRouterInterfaceIpEvent(String apiId) {
        super(apiId);
    }

    public static APISetL3NetworkRouterInterfaceIpEvent __example__() {
        APISetL3NetworkRouterInterfaceIpEvent event = new APISetL3NetworkRouterInterfaceIpEvent();
        event.setSuccess(true);
        return event;
    }
}
