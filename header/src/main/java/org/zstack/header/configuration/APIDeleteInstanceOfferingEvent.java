package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteInstanceOfferingEvent extends APIEvent {
    public APIDeleteInstanceOfferingEvent() {
        super(null);
    }

    public APIDeleteInstanceOfferingEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteInstanceOfferingEvent __example__() {
        APIDeleteInstanceOfferingEvent event = new APIDeleteInstanceOfferingEvent();


        return event;
    }

}
