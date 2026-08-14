package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteDiskOfferingEvent extends APIEvent {
    public APIDeleteDiskOfferingEvent() {
        super(null);
    }

    public APIDeleteDiskOfferingEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteDiskOfferingEvent __example__() {
        APIDeleteDiskOfferingEvent event = new APIDeleteDiskOfferingEvent();


        return event;
    }

}
