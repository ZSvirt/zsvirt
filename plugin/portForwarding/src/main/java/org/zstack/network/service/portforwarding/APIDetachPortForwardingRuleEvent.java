package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 * api event for message :ref:`APIDetachPortForwardingRuleMsg`
 *
 *@category port forwarding
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.portforwarding.APIDetachPortForwardingRuleEvent": {
"inventory": {
"uuid": "26679c3eb6694a5e8e3528e5f7afe6d1",
"name": "pfRule1",
"vipUuid": "a49d1fcdb3e53c89b0e7b38494a363b5",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"protocolType": "TCP",
"allowedCidr": "77.10.3.1/24",
"createDate": "May 13, 2014 11:22:00 PM",
"lastOpDate": "May 13, 2014 11:22:00 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIDetachPortForwardingRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`PortForwardingRuleInventory`
     */
    private PortForwardingRuleInventory inventory;

    public APIDetachPortForwardingRuleEvent(String apiId) {
        super(apiId);
    }

    public APIDetachPortForwardingRuleEvent() {
        super(null);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachPortForwardingRuleEvent __example__() {
        APIDetachPortForwardingRuleEvent event = new APIDetachPortForwardingRuleEvent();
        PortForwardingRuleInventory rule = new PortForwardingRuleInventory();
        rule.setUuid(uuid());
        rule.setName("TestAttachRule");
        rule.setDescription("test atatch rule");
        rule.setAllowedCidr("0.0.0.0/0");
        rule.setGuestIp("10.0.0.244");
        rule.setPrivatePortStart(33);
        rule.setPrivatePortEnd(33);
        rule.setProtocolType("TCP");
        rule.setState(PortForwardingRuleState.Enabled.toString());
        rule.setVipPortStart(33);
        rule.setVipPortEnd(33);
        rule.setVipIp("192.168.0.187");
        rule.setVipUuid(uuid());
        rule.setVmNicUuid(uuid());
        rule.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        rule.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(rule);
        return event;
    }

}
