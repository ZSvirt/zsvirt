package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteReservedIpRangeEvent extends APIEvent {
    public APIDeleteReservedIpRangeEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteReservedIpRangeEvent() {
        super(null);
    }
 
    public static APIDeleteReservedIpRangeEvent __example__() {
        APIDeleteReservedIpRangeEvent event = new APIDeleteReservedIpRangeEvent();
        event.setSuccess(true);
        return event;
    }

}
