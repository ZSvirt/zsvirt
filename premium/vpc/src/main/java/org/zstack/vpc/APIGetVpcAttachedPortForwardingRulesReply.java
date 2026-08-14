package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleState;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin.ruan 2021/03/23
 */
@RestResponse(allTo = "inventories")
public class APIGetVpcAttachedPortForwardingRulesReply extends APIReply {
    private List<PortForwardingRuleInventory> inventories;

    public List<PortForwardingRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PortForwardingRuleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcAttachedPortForwardingRulesReply __example__() {
        APIGetVpcAttachedPortForwardingRulesReply reply = new APIGetVpcAttachedPortForwardingRulesReply();

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

        reply.setInventories(Arrays.asList(rule));
        return reply;
    }
}
