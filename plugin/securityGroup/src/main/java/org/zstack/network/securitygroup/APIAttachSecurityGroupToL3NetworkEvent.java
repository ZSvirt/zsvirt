package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 *
 * api event for :ref:`APIAttachSecurityGroupToL3NetworkMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.network.securitygroup.APIAttachSecurityGroupToL3NetworkEvent": {
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
"attachedL3NetworkUuids": [
"a17f2ea774ba41caadea0b937a7329a3"
]
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIAttachSecurityGroupToL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;

    public APIAttachSecurityGroupToL3NetworkEvent() {
        super(null);
    }
    
    public APIAttachSecurityGroupToL3NetworkEvent(String apiId) {
        super(apiId);
    }
    
    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachSecurityGroupToL3NetworkEvent __example__() {
        APIAttachSecurityGroupToL3NetworkEvent event = new APIAttachSecurityGroupToL3NetworkEvent();
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
