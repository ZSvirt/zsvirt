package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 *
 * api event for :ref:`APIDeleteSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupEvent": {
"success": true
}
}
 */
@RestResponse
public class APIDeleteSecurityGroupEvent extends APIEvent {
    public APIDeleteSecurityGroupEvent() {
        super(null);
    }
    
    public APIDeleteSecurityGroupEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteSecurityGroupEvent __example__() {
        APIDeleteSecurityGroupEvent event = new APIDeleteSecurityGroupEvent();
        event.setSuccess(true);
        return event;
    }

}
