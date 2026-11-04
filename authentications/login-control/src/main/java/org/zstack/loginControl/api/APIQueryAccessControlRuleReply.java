package org.zstack.loginControl.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.loginControl.entity.AccessControlRuleInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryAccessControlRuleReply extends APIQueryReply {
    private List<AccessControlRuleInventory> inventories;

    public List<AccessControlRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccessControlRuleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAccessControlRuleReply __example__() {
        APIQueryAccessControlRuleReply reply = new APIQueryAccessControlRuleReply();

        AccessControlRuleInventory rule = new AccessControlRuleInventory();

        rule.setRule("192.168.10.0/24");

        reply.setInventories(asList(rule));
        return reply;
    }
}
