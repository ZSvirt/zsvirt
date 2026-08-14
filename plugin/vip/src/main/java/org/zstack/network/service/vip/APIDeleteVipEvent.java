package org.zstack.network.service.vip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for message :ref:`APIDeleteVipMsg`
 *
 *@category vip
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.network.service.vip.APIDeleteVipEvent": {
"success": true
}
}
 *
 */
@RestResponse
public class APIDeleteVipEvent extends APIEvent {
    public APIDeleteVipEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeleteVipEvent() {
        super(null);
    }
 
    public static APIDeleteVipEvent __example__() {
        APIDeleteVipEvent event = new APIDeleteVipEvent();


        return event;
    }

}
