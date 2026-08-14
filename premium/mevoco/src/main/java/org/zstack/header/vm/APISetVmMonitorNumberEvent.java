package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by root on 7/29/16.
 */
@RestResponse
public class APISetVmMonitorNumberEvent extends APIEvent {
    public APISetVmMonitorNumberEvent() {
    }

    public APISetVmMonitorNumberEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmMonitorNumberEvent __example__() {
        APISetVmMonitorNumberEvent event = new APISetVmMonitorNumberEvent();
        return event;
    }

}
