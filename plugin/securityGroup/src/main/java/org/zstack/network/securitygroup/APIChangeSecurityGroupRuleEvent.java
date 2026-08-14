package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 * api event for :ref:`APIChangeSecurityGroupRuleMsg`
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIChangeSecurityGroupRuleEvent": {
"inventory": {
"uuid": "02bc62abee88444ca3e2c434a1b8fdea",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"srcIpRange": "10.10.10.1-10.10.10.10",
"dstPortRange": "10-100",
"protocol": "UDP",
"action": "DROP"
"createDate": "Jul 10, 2023 9:38:24 PM",
"lastOpDate": "Jul 10, 2023 9:38:24 PM",
"state": "Enabled",
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIChangeSecurityGroupRuleEvent extends APIEvent {
    /**
     * @desc :ref:`SecurityGroupRuleInventory`
     */
    private SecurityGroupRuleInventory inventory;

    public APIChangeSecurityGroupRuleEvent() {
        super(null);
    }

    public APIChangeSecurityGroupRuleEvent(String apiId) {
        super(apiId);
    }

    public SecurityGroupRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupRuleInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeSecurityGroupRuleEvent __example__() {
        APIChangeSecurityGroupRuleEvent event = new APIChangeSecurityGroupRuleEvent();
        SecurityGroupRuleInventory rule = new SecurityGroupRuleInventory();
        rule.setUuid(uuid());
        rule.setSecurityGroupUuid(uuid());
        rule.setProtocol("tcp");
        rule.setSrcIpRange("10.10.10.1-10.10.10.10");
        rule.setDstPortRange("2001-2023");
        rule.setAction("RETURN");
        rule.setType("ingress");
        rule.setState("enable");
        rule.setCreateDate(new Timestamp(DocUtils.date));
        rule.setLastOpDate(new Timestamp(DocUtils.date));
        event.setInventory(rule);
        return event;
    }
}