package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 2/26/2016.
 */
@RestResponse
public class APISetVmHostnameEvent extends APIEvent {
    public APISetVmHostnameEvent() {
    }

    public APISetVmHostnameEvent(String apiId) {
        super(apiId);
    }
 
    public static APISetVmHostnameEvent __example__() {
        APISetVmHostnameEvent event = new APISetVmHostnameEvent();


        return event;
    }

}
