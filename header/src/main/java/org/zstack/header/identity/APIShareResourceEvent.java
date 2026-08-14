package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/13/2015.
 */
@RestResponse
public class APIShareResourceEvent extends APIEvent {
    public APIShareResourceEvent() {
    }

    public APIShareResourceEvent(String apiId) {
        super(apiId);
    }
 
    public static APIShareResourceEvent __example__() {
        APIShareResourceEvent event = new APIShareResourceEvent();


        return event;
    }

}
