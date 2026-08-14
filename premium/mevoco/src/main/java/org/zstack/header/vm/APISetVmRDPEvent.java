package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by root on 7/29/16.
 */
@RestResponse
public class APISetVmRDPEvent extends APIEvent {
    public APISetVmRDPEvent() {
    }

    public APISetVmRDPEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmRDPEvent __example__() {
        APISetVmRDPEvent event = new APISetVmRDPEvent();
        return event;
    }

}
