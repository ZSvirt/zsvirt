package org.zstack.header.affinitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin on 10/25/2017.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAffinityGroupEvent extends APIEvent {
    private AffinityGroupInventory inventory;

    public AffinityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AffinityGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAffinityGroupEvent() {
    }

    public APIUpdateAffinityGroupEvent(String apiId) {
        super(apiId);
    }
 
    public static APIUpdateAffinityGroupEvent __example__() {
        APIUpdateAffinityGroupEvent event = new APIUpdateAffinityGroupEvent();
        AffinityGroupInventory inventory = new AffinityGroupInventory();
        inventory.setName("affinity group");
        inventory.setDescription("affinity group for test");
        inventory.setUuid(uuid());
        inventory.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
        inventory.setType("1.0");
        inventory.setType(AffinityGroupType.HOST.toString());
        event.setInventory(inventory);

        return event;
    }

}
