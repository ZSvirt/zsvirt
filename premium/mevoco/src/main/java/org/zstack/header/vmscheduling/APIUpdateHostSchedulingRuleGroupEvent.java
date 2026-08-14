package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestResponse(allTo="inventory")
public class APIUpdateHostSchedulingRuleGroupEvent extends APIEvent {
    private HostSchedulingRuleGroupInventory inventory;

    public APIUpdateHostSchedulingRuleGroupEvent() {
    }

    public APIUpdateHostSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public HostSchedulingRuleGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostSchedulingRuleGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHostSchedulingRuleGroupEvent __example__() {
        APIUpdateHostSchedulingRuleGroupEvent event = new APIUpdateHostSchedulingRuleGroupEvent();
        HostSchedulingRuleGroupInventory inventory = new HostSchedulingRuleGroupInventory();
        inventory.setUuid(uuid());
        inventory.setName("test");
        inventory.setDescription("desc");
        inventory.setZoneUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
