package org.zstack.header.zone;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


/**
 * @apiResult api event for message :ref:`APIDeleteZoneMsg`
 * @example {
 * "org.zstack.header.zone.APIDeleteZoneEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeleteZoneEvent extends APIEvent {

    public APIDeleteZoneEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteZoneEvent() {
        super(null);
    }
 
    public static APIDeleteZoneEvent __example__() {
        APIDeleteZoneEvent event = new APIDeleteZoneEvent();
        event.setSuccess(true);
        return event;
    }

}
