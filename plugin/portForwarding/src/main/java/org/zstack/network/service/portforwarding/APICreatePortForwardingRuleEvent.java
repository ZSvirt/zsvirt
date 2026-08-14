package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 *@apiResult
 *
 * api event for message :ref:`APICreatePortForwardingRuleMsg`
 *
 *@category port forwarding
 *
 *@since 0.1.0
 *
 *@example
 *
 * {
"org.zstack.network.service.portforwarding.APICreatePortForwardingRuleEvent": {
"inventory": {
"uuid": "5ddaefbaba7d46d889aa3f3a6f50f6c8",
"name": "pfRule1",
"vipUuid": "22647f340e1037d4a2ea499aca42075e",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"vmNicUuid": "bd00f2c066c94f07b0dfae2e9e9b567f",
"protocolType": "TCP",
"allowedCidr": "77.10.3.1/24"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APICreatePortForwardingRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`PortForwardingRuleInventory`
     */
    private PortForwardingRuleInventory inventory;
    
    public APICreatePortForwardingRuleEvent(String apiId) {
        super(apiId);
    }
    
    public APICreatePortForwardingRuleEvent() {
        super(null);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreatePortForwardingRuleEvent __example__() {
        APICreatePortForwardingRuleEvent event = new APICreatePortForwardingRuleEvent();
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
