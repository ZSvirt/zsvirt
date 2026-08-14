package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 09/03/2017.
 */
@RestResponse(allTo = "inventory")
public class APICreateVniRangeEvent extends APIEvent {
    private VniRangeInventory inventory;

    public APICreateVniRangeEvent(String apiId) {
        super(apiId);
    }

    public APICreateVniRangeEvent() {
        super(null);
    }

    public VniRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VniRangeInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVniRangeEvent __example__() {
        APICreateVniRangeEvent event = new APICreateVniRangeEvent();
        VniRangeInventory inv = new VniRangeInventory();

        inv.setName("TestVniRange");
        inv.setDescription("Here is a Vni Range");
        inv.setStartVni(10);
        inv.setEndVni(5000);
        inv.setL2NetworkUuid(uuid());

        event.setInventory(inv);
        return event;
    }
}
