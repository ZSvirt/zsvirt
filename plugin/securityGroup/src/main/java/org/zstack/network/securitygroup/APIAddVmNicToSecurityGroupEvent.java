package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 *
 * api event for :ref:`APIAddVmNicToSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIAddVmNicToSecurityGroupEvent": {
"success": true
}
}
 */
@RestResponse
public class APIAddVmNicToSecurityGroupEvent extends APIEvent {
    public APIAddVmNicToSecurityGroupEvent(String apiId) {
        super(apiId);
    }
    
    public APIAddVmNicToSecurityGroupEvent() {
    }
 
    public static APIAddVmNicToSecurityGroupEvent __example__() {
        APIAddVmNicToSecurityGroupEvent event = new APIAddVmNicToSecurityGroupEvent();
        event.setSuccess(true);
        return event;
    }
}
