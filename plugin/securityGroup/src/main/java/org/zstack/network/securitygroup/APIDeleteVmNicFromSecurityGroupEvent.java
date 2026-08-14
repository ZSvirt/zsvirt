package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 *
 * api event for :ref:`APIDeleteVmNicFromSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDeleteVmNicFromSecurityGroupEvent": {
"success": true
}
}
 */

@RestResponse
public class APIDeleteVmNicFromSecurityGroupEvent extends APIEvent {
    public APIDeleteVmNicFromSecurityGroupEvent() {
        super(null);
    }
    
    public APIDeleteVmNicFromSecurityGroupEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteVmNicFromSecurityGroupEvent __example__() {
        APIDeleteVmNicFromSecurityGroupEvent event = new APIDeleteVmNicFromSecurityGroupEvent();
        event.setSuccess(true);
        return event;
    }

}
