package org.zstack.header.affinitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 */
@RestResponse(allTo = "inventory")
public class APIChangeAffinityGroupStateEvent extends APIEvent {
    private AffinityGroupInventory inventory;

    public APIChangeAffinityGroupStateEvent() {
        super(null);
    }

    public APIChangeAffinityGroupStateEvent(String apiId) {
        super(apiId);
    }

    public AffinityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AffinityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeAffinityGroupStateEvent __example__() {
        APIChangeAffinityGroupStateEvent event = new APIChangeAffinityGroupStateEvent();
        AffinityGroupInventory ag = new AffinityGroupInventory();

        ag.setName("Test-AffinityGroup");
        ag.setAppliance("CUSTOMER");
        ag.setDescription("Test-AffinityGroup");
        ag.setType("HOST");
        ag.setVersion("1.0");
        ag.setUuid(uuid());
        ag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(ag);
        return event;
    }

}

