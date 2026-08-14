package org.zstack.header.protocol;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVRouterOspfAreaEvent extends APIEvent {
    public APIDeleteVRouterOspfAreaEvent() {
        super(null);
    }

    public APIDeleteVRouterOspfAreaEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVRouterOspfAreaEvent __example__() {
        APIDeleteVRouterOspfAreaEvent event = new APIDeleteVRouterOspfAreaEvent();
        event.setSuccess(true);
        return event;
    }

}
