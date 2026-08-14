package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdatePortForwardingRuleEvent extends APIEvent {
    private PortForwardingRuleInventory inventory;

    public APIUpdatePortForwardingRuleEvent() {
    }

    public APIUpdatePortForwardingRuleEvent(String apiId) {
        super(apiId);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdatePortForwardingRuleEvent __example__() {
        APIUpdatePortForwardingRuleEvent event = new APIUpdatePortForwardingRuleEvent();
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
