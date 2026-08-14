package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APIDeleteIpRangeMsg`
 * @category l3Network
 * @example {
 * "org.zstack.header.network.l3.APIDeleteIpRangeEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse
public class APIDeleteIpRangeEvent extends APIEvent {
    public APIDeleteIpRangeEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteIpRangeEvent() {
        super(null);
    }
 
    public static APIDeleteIpRangeEvent __example__() {
        APIDeleteIpRangeEvent event = new APIDeleteIpRangeEvent();
        event.setSuccess(true);
        return event;
    }

}
