package org.zstack.header.vmscheduling;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author shenjin
 * @date 2023/5/5 16:47
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmSchedulingRuleReply extends APIQueryReply {
    private List<VmSchedulingRuleInventory> inventories;

    public List<VmSchedulingRuleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmSchedulingRuleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmSchedulingRuleReply __example__() {
        APIQueryVmSchedulingRuleReply reply = new APIQueryVmSchedulingRuleReply();
        VmSchedulingRuleInventory inventory = new VmSchedulingRuleInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        inventory.setRule("rule");
        inventory.setMode("mode");
        inventory.setDescription("description");
        reply.setInventories(asList(inventory));
        return reply;
    }
}
