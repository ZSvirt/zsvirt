package org.zstack.header.affinitygroup;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


@RestResponse(allTo = "inventory")
public class APIRemoveVmFromAffinityGroupEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeInventory`
     */
    private AffinityGroupInventory inventory;

    public APIRemoveVmFromAffinityGroupEvent(String apiId) {
        super(apiId);
    }

    public APIRemoveVmFromAffinityGroupEvent() {
        super(null);
    }

    public AffinityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AffinityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveVmFromAffinityGroupEvent __example__() {
        APIRemoveVmFromAffinityGroupEvent event = new APIRemoveVmFromAffinityGroupEvent();
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
