package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestResponse(allTo="inventory")
public class APICreateHostSchedulingRuleGroupEvent extends APIEvent {
    private HostSchedulingRuleGroupInventory inventory;

    public APICreateHostSchedulingRuleGroupEvent() {
    }

    public APICreateHostSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public HostSchedulingRuleGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostSchedulingRuleGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateHostSchedulingRuleGroupEvent __example__() {
        APICreateHostSchedulingRuleGroupEvent event = new APICreateHostSchedulingRuleGroupEvent();
        HostSchedulingRuleGroupInventory inventory = new HostSchedulingRuleGroupInventory();
        inventory.setZoneUuid(uuid());
        inventory.setName("test");
        inventory.setDescription("test desc");
        event.setInventory(inventory);
        return event;
    }
}
