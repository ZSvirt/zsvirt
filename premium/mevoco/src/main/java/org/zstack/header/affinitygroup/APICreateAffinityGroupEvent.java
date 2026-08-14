package org.zstack.header.affinitygroup;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin on 10/24/2017.
 */
@RestResponse(allTo = "inventory")
public class APICreateAffinityGroupEvent extends APIEvent {
    private AffinityGroupInventory inventory;

    public APICreateAffinityGroupEvent() {
    }

    public APICreateAffinityGroupEvent(String apiId) {
        super(apiId);
    }

    public AffinityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AffinityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateAffinityGroupEvent __example__() {
        APICreateAffinityGroupEvent event = new APICreateAffinityGroupEvent();
        AffinityGroupInventory affinityGroup = new AffinityGroupInventory();
        affinityGroup.setUuid(uuid());
        affinityGroup.setName("affinity-group-test");
        affinityGroup.setDescription("affinity group for test");
        affinityGroup.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
        affinityGroup.setType(AffinityGroupType.HOST.toString());
        affinityGroup.setVersion("1.0");
        event.setInventory(affinityGroup);

        return event;
    }

}
