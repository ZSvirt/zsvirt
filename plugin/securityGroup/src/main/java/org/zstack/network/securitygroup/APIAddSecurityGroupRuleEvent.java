package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 * api event for :ref:`APIAddSecurityGroupRuleMsg`
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIAddSecurityGroupRuleEvent": {
"inventory": {
"uuid": "3904b4837f0c4f539063777ed463b648",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM",
"internalId": 1,
"rules": [
{
"uuid": "ca69dcedbb4f407c9a62240bc54fd6ba",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"startPort": 22,
"endPort": 100,
"protocol": "TCP",
"allowedCidr": "0.0.0.0/0",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM"
},
{
"uuid": "02bc62abee88444ca3e2c434a1b8fdea",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"startPort": 10,
"endPort": 10,
"protocol": "UDP",
"allowedCidr": "192.168.0.1/0",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM"
}
],
"attachedL3NetworkUuids": []
},
"success": true
}
}
 */

@RestResponse(allTo = "inventory")
public class APIAddSecurityGroupRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;
    
    public APIAddSecurityGroupRuleEvent(String apiId) {
        super(apiId);
    }
    
    public APIAddSecurityGroupRuleEvent() {
        super(null);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddSecurityGroupRuleEvent __example__() {
        APIAddSecurityGroupRuleEvent event = new APIAddSecurityGroupRuleEvent();
        SecurityGroupInventory sec = new SecurityGroupInventory();
        sec.setUuid(uuid());
        sec.setName("web");
        sec.setDescription("for test");
        sec.setState(SecurityGroupRuleState.Enabled.toString());
        sec.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        sec.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(sec);
        event.setSuccess(true);
        return event;
    }

}
