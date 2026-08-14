package org.zstack.header.affinitygroup;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import static java.util.Arrays.asList;


@RestResponse(allTo = "inventory")
public class APIAddVmToAffinityGroupEvent extends APIEvent {
    /**
     * @desc see :ref:`VmInstanceInventory`
     */
    private AffinityGroupInventory inventory;

    public APIAddVmToAffinityGroupEvent() {
    }

    public APIAddVmToAffinityGroupEvent(String apiId) {
        super(apiId);
    }

    public AffinityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AffinityGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddVmToAffinityGroupEvent __example__() {
        APIAddVmToAffinityGroupEvent event = new APIAddVmToAffinityGroupEvent();

        AffinityGroupInventory inv = new AffinityGroupInventory();
        inv.setUuid(uuid());
        inv.setName("affinity-group-test");
        inv.setDescription("affinity group for test");
        inv.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
        inv.setType(AffinityGroupType.HOST.toString());
        inv.setVersion("1.0");

        AffinityGroupUsageInventory usageInv = new AffinityGroupUsageInventory();
        usageInv.setUuid(inv.getUuid());
        usageInv.setResourceType(AffinityGroupType.HOST.toString());
        usageInv.setResourceUuid(uuid());

        inv.setUsages(asList(usageInv));
        event.setInventory(inv);

        return event;
    }

}
