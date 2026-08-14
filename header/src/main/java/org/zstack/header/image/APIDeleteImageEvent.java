package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteImageEvent extends APIEvent {

    public APIDeleteImageEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteImageEvent() {
        super(null);
    }

 
    public static APIDeleteImageEvent __example__() {
        APIDeleteImageEvent event = new APIDeleteImageEvent();


        return event;
    }

}
