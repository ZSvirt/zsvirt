package org.zstack.header.vmscheduling;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author shenjin
 * @date 2023/5/5 16:46
 */
@RestResponse(allTo = "inventories")
public class APIQueryHostSchedulingRuleGroupReply extends APIQueryReply {
    private List<HostSchedulingRuleGroupInventory> inventories;

    public List<HostSchedulingRuleGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostSchedulingRuleGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostSchedulingRuleGroupReply __example__() {
        APIQueryHostSchedulingRuleGroupReply reply = new APIQueryHostSchedulingRuleGroupReply();
        HostSchedulingRuleGroupInventory inventory = new HostSchedulingRuleGroupInventory();
        inventory.setName("test");
        inventory.setDescription("description");
        inventory.setUuid(uuid());
        inventory.setZoneUuid(uuid());
        reply.setInventories(asList(inventory));
        return reply;
    }
}
