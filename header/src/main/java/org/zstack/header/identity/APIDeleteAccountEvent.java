package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/15/2015.
 */
@RestResponse
public class APIDeleteAccountEvent extends APIEvent {
    public APIDeleteAccountEvent() {
    }

    public APIDeleteAccountEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteAccountEvent __example__() {
        APIDeleteAccountEvent event = new APIDeleteAccountEvent();


        return event;
    }

}
