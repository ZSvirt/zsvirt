package org.zstack.header.bonding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteBondingEvent extends APIEvent {
    public APIDeleteBondingEvent() {
    }

    public  APIDeleteBondingEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteBondingEvent __example__() {
        APIDeleteBondingEvent evt = new APIDeleteBondingEvent();
        evt.setSuccess(true);
        return evt;
    }
}
