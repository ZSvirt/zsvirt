package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APIDeleteL3NetworkMsg`
 * @category l3Network
 * @example {
 * "org.zstack.header.network.l3.APIDeleteL3NetworkEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeleteL3NetworkEvent extends APIEvent {
    public APIDeleteL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteL3NetworkEvent() {
        super(null);
    }
 
    public static APIDeleteL3NetworkEvent __example__() {
        APIDeleteL3NetworkEvent event = new APIDeleteL3NetworkEvent();
        event.setSuccess(true);
        return event;
    }

}
