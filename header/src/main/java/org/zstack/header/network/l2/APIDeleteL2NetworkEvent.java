package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDeleteL2NetworkMsg`
 * @category l2Network
 * @example {
 * "org.zstack.header.network.l2.APIDeleteL2NetworkEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDeleteL2NetworkEvent extends APIEvent {
    public APIDeleteL2NetworkEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteL2NetworkEvent() {
        super(null);
    }
 
    public static APIDeleteL2NetworkEvent __example__() {
        APIDeleteL2NetworkEvent event = new APIDeleteL2NetworkEvent();
        event.setSuccess(true);

        return event;
    }

}
