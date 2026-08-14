package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestResponse
public class APISetImageQgaEvent extends APIEvent {
    public APISetImageQgaEvent() {
    }

    public APISetImageQgaEvent(String apiId) {
        super(apiId);
    }
 
    public static APISetImageQgaEvent __example__() {
        APISetImageQgaEvent event = new APISetImageQgaEvent();


        return event;
    }

}
