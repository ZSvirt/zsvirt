package org.zstack.header.vpc.ha;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


@RestResponse
public class APIDeleteVpcHaGroupEvent extends APIEvent {

    public APIDeleteVpcHaGroupEvent() {
        super(null);
    }

    public APIDeleteVpcHaGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVpcHaGroupEvent __example__() {
        APIDeleteVpcHaGroupEvent event = new APIDeleteVpcHaGroupEvent();
        return event;
    }

}
