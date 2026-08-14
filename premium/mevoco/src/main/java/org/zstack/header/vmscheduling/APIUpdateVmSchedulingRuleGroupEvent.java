package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestResponse(allTo="inventory")
public class APIUpdateVmSchedulingRuleGroupEvent extends APIEvent {
    private VmSchedulingRuleGroupInventory inventory;


    public APIUpdateVmSchedulingRuleGroupEvent() {
    }

    public APIUpdateVmSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public VmSchedulingRuleGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmSchedulingRuleGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVmSchedulingRuleGroupEvent __example__() {
        APIUpdateVmSchedulingRuleGroupEvent event = new APIUpdateVmSchedulingRuleGroupEvent();
        VmSchedulingRuleGroupInventory inventory = new VmSchedulingRuleGroupInventory();
        inventory.setUuid(uuid());
        inventory.setZoneUuid(uuid());
        inventory.setName("test");
        inventory.setDescription("test desc");
        inventory.setAppliance("CUSTOMER");
        event.setInventory(inventory);

        return event;
    }
}
