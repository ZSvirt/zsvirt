package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 * api event for message :ref:`APIAttachPortForwardingRuleMsg`
 *
 *@category port forwarding
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleEvent": {
"inventory": {
"uuid": "bc82d5c4f9394c24b7fa19ee611c0857",
"name": "pfRule1",
"vipUuid": "7b5216172fe83c05940e15c629922a79",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"vmNicUuid": "5dfef29a376a49de9e1a887ea9bfe683",
"protocolType": "TCP",
"allowedCidr": "77.10.3.1/24",
"createDate": "May 6, 2014 11:07:57 PM",
"lastOpDate": "May 6, 2014 11:07:57 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIAttachPortForwardingRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`PortForwardingRuleInventory`
     */
    private PortForwardingRuleInventory inventory;

    public APIAttachPortForwardingRuleEvent(String apiId) {
        super(apiId);
    }

    public APIAttachPortForwardingRuleEvent() {
        super(null);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachPortForwardingRuleEvent __example__() {
        APIAttachPortForwardingRuleEvent event = new APIAttachPortForwardingRuleEvent();
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
        event.setSuccess(true);


        return event;
    }

}
