package org.zstack.header.vmscheduling;

import org.zstack.header.affinitygroup.AffinityGroupPolicy;
import org.zstack.header.affinitygroup.AffinityGroupType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVmSchedulingRuleEvent extends APIEvent {
    private VmSchedulingRuleInventory inventory;

    public APIUpdateVmSchedulingRuleEvent() {
    }

    public APIUpdateVmSchedulingRuleEvent(String apiId) {
        super(apiId);
    }

    public VmSchedulingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmSchedulingRuleInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVmSchedulingRuleEvent __example__() {
        APIUpdateVmSchedulingRuleEvent event = new APIUpdateVmSchedulingRuleEvent();
        VmSchedulingRuleInventory inventory = new VmSchedulingRuleInventory();
        inventory.setName("group");
        inventory.setDescription("group for test");
        inventory.setUuid(uuid());
        inventory.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
        inventory.setType("1.0");
        inventory.setType(AffinityGroupType.HOST.toString());
        inventory.setRule("AFFINITY");
        inventory.setMode("SOFT");
        event.setInventory(inventory);

        return event;
    }
}
