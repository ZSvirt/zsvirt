package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for message :ref:`APIDeletePortForwardingRuleMsg`
 *
 *@category port forwarding
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.portforwarding.APIDeletePortForwardingRuleEvent": {
"success": true
}
}
 */
@RestResponse
public class APIDeletePortForwardingRuleEvent extends APIEvent {
    public APIDeletePortForwardingRuleEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeletePortForwardingRuleEvent() {
        super(null);
    }
 
    public static APIDeletePortForwardingRuleEvent __example__() {
        APIDeletePortForwardingRuleEvent event = new APIDeletePortForwardingRuleEvent();
        event.setSuccess(true);
        return event;
    }

}
