package org.zstack.billing;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteBillingEvent extends APIEvent {
    public APIDeleteBillingEvent() {
    }

    public APIDeleteBillingEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteBillingEvent __example__() {
        APIDeleteBillingEvent event = new APIDeleteBillingEvent();


        return event;
    }

}
