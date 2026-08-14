package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 * api event for :ref:`APIDeleteSecurityGroupRuleMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleEvent": {
"inventory": {
"uuid": "bb0538d800e54497b50710f449056a9f",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 10:36:48 PM",
"lastOpDate": "May 14, 2014 10:36:48 PM",
"internalId": 1,
"rules": [
{
"uuid": "4248355e4a534a2c8b0e9986ab0d00bc",
"securityGroupUuid": "bb0538d800e54497b50710f449056a9f",
"type": "Ingress",
"priority": 1,
"protocol": "TCP",
"srcIpRange": "10.10.10.1,10.10.10.2",
"dstIpRange": "20.20.20.1,20.20.20.1",
"srcPortRange": "10,20",
"dstPortRange": "30,40",
"defaultTarget": "RETURN",
"state": "Enabled",
"remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
"createDate": "May 14, 2014 10:36:48 PM",
"lastOpDate": "May 14, 2014 10:36:48 PM"
}
],
"attachedL3NetworkUuids": [
"c345b97c7fa4400687fcecef64949a83"
]
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIDeleteSecurityGroupRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;
    
    public APIDeleteSecurityGroupRuleEvent(String apiId) {
        super(apiId);
    }
    
    public APIDeleteSecurityGroupRuleEvent() {
        super(null);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDeleteSecurityGroupRuleEvent __example__() {
        APIDeleteSecurityGroupRuleEvent event = new APIDeleteSecurityGroupRuleEvent();
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
