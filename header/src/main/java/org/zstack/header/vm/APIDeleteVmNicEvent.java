package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVmNicEvent extends APIEvent {
    public APIDeleteVmNicEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVmNicEvent() {
        super(null);
    }
 
    public static APIDeleteVmNicEvent __example__() {
        APIDeleteVmNicEvent event = new APIDeleteVmNicEvent();


        return event;
    }

}
