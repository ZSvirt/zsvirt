package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDeleteHostMsg`
 * @example {
 * "org.zstack.header.host.APIDeleteHostEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeleteHostEvent extends APIEvent {
    public APIDeleteHostEvent() {
        super(null);
    }

    public APIDeleteHostEvent(String apiId) {
        super(apiId);
    }

 
    public static APIDeleteHostEvent __example__() {
        APIDeleteHostEvent event = new APIDeleteHostEvent();


        return event;
    }

}
