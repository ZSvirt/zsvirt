package org.zstack.header.affinitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteAffinityGroupEvent extends APIEvent {
    public APIDeleteAffinityGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAffinityGroupEvent() {
        super(null);
    }
 
    public static APIDeleteAffinityGroupEvent __example__() {
        APIDeleteAffinityGroupEvent event = new APIDeleteAffinityGroupEvent();
        event.setSuccess(true);
        return event;
    }

}
