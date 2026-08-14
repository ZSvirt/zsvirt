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
public class APIQueryVmSchedulingRuleGroupReply extends APIQueryReply {
    private List<VmSchedulingRuleGroupInventory> inventories;

    public List<VmSchedulingRuleGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmSchedulingRuleGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmSchedulingRuleGroupReply __example__() {
        APIQueryVmSchedulingRuleGroupReply reply = new APIQueryVmSchedulingRuleGroupReply();
        VmSchedulingRuleGroupInventory inventory = new VmSchedulingRuleGroupInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        inventory.setAppliance("appliance");
        inventory.setDescription("description");
        inventory.setZoneUuid(uuid());
        reply.setInventories(asList(inventory));
        return reply;
    }
}
