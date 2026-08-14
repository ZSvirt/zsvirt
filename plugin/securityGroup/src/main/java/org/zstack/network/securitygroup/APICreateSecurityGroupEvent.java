package org.zstack.network.securitygroup;

import org.junit.Test;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 *
 * api event for :ref:`APICreateSecurityGroupMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APICreateSecurityGroupEvent": {
"inventory": {
"uuid": "3904b4837f0c4f539063777ed463b648",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM",
"internalId": 1,
"rules": [],
"attachedL3NetworkUuids": []
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APICreateSecurityGroupEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;

    public APICreateSecurityGroupEvent(String apiId) {
        super(apiId);
    }
    
    public APICreateSecurityGroupEvent() {
    }
    
    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateSecurityGroupEvent __example__() {
        //todo make sure empty attachedL3NetworkUuids and rules no need to set
        APICreateSecurityGroupEvent event = new APICreateSecurityGroupEvent();
        SecurityGroupInventory sec = new SecurityGroupInventory();
        sec.setUuid(uuid());
        sec.setName("web");
        sec.setDescription("for test");
        sec.setState(SecurityGroupState.Enabled.toString());
        sec.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        sec.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(sec);
        event.setSuccess(true);
        return event;
    }

}
