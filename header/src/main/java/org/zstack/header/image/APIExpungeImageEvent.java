package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 11/15/2015.
 */
@RestResponse
public class APIExpungeImageEvent extends APIEvent {
    public APIExpungeImageEvent() {
    }

    public APIExpungeImageEvent(String apiId) {
        super(apiId);
    }
 
    public static APIExpungeImageEvent __example__() {
        APIExpungeImageEvent event = new APIExpungeImageEvent();


        return event;
    }

}
