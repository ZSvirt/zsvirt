package org.zstack.network.service.eip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for :ref:`APIDeleteEipMsg`
 *
 *@category eip
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.eip.APIDeleteEipEvent": {
"success": true
}
}
 */
@RestResponse
public class APIDeleteEipEvent extends APIEvent {
    public APIDeleteEipEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteEipEvent() {
    }
 
    public static APIDeleteEipEvent __example__() {
        APIDeleteEipEvent event = new APIDeleteEipEvent();
        event.setSuccess(true);

        return event;
    }

}
