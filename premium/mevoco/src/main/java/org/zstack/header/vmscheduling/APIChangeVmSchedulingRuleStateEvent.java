package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@RestResponse(allTo = "inventory")
public class APIChangeVmSchedulingRuleStateEvent extends APIEvent {
    private VmSchedulingRuleInventory inventory;

    public APIChangeVmSchedulingRuleStateEvent() {
        super(null);
    }

    public APIChangeVmSchedulingRuleStateEvent(String apiId) {
        super(apiId);
    }

    public VmSchedulingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmSchedulingRuleInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVmSchedulingRuleStateEvent __example__() {
        APIChangeVmSchedulingRuleStateEvent event = new APIChangeVmSchedulingRuleStateEvent();
        VmSchedulingRuleInventory ag = new VmSchedulingRuleInventory();

        ag.setName("Test-AffinityGroup");
        ag.setAppliance("CUSTOMER");
        ag.setDescription("Test-AffinityGroup");
        ag.setType("HOST");
        ag.setVersion("1.0");
        ag.setUuid(uuid());
        ag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ag.setRule("AFFINITY");
        ag.setMode("SOFT");

        event.setInventory(ag);
        return event;
    }
}
