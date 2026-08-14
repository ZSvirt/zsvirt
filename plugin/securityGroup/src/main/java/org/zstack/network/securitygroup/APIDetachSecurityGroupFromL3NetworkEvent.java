package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 * api event for :ref:`APIDetachSecurityGroupFromL3NetworkMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIDetachSecurityGroupFromL3NetworkEvent": {
"inventory": {
"uuid": "3d9d1a6a472440a4b53d2389c111336c",
"name": "test",
"state": "Enabled",
"createDate": "May 14, 2014 11:00:16 PM",
"lastOpDate": "May 14, 2014 11:00:16 PM",
"internalId": 1,
"rules": [
{
"uuid": "9f4171342f9844199002ab78bc3680c9",
"securityGroupUuid": "3d9d1a6a472440a4b53d2389c111336c",
"type": "Ingress",
"startPort": 22,
"endPort": 100,
"protocol": "TCP",
"allowedCidr": "0.0.0.0/0",
"createDate": "May 14, 2014 11:00:16 PM",
"lastOpDate": "May 14, 2014 11:00:16 PM"
},
{
"uuid": "dc362d94604d4673ac780adbaba0f22c",
"securityGroupUuid": "3d9d1a6a472440a4b53d2389c111336c",
"type": "Ingress",
"startPort": 10,
"endPort": 10,
"protocol": "UDP",
"allowedCidr": "192.168.0.1/0",
"createDate": "May 14, 2014 11:00:16 PM",
"lastOpDate": "May 14, 2014 11:00:16 PM"
}
],
"attachedL3NetworkUuids": [
"12b707b8522b40468631906d1f10f4cc"
]
},
"success": true
}
}
 */

@RestResponse(allTo = "inventory")
public class APIDetachSecurityGroupFromL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;

    public APIDetachSecurityGroupFromL3NetworkEvent() {
        super(null);
    }

    public APIDetachSecurityGroupFromL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachSecurityGroupFromL3NetworkEvent __example__() {
        APIDetachSecurityGroupFromL3NetworkEvent event = new APIDetachSecurityGroupFromL3NetworkEvent();
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
