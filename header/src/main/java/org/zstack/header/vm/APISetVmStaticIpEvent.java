package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 2/26/2016.
 */
@RestResponse
public class APISetVmStaticIpEvent extends APIEvent {
    public APISetVmStaticIpEvent() {
    }

    public APISetVmStaticIpEvent(String apiId) {
        super(apiId);
    }
 
    public static APISetVmStaticIpEvent __example__() {
        APISetVmStaticIpEvent event = new APISetVmStaticIpEvent();


        return event;
    }

}
