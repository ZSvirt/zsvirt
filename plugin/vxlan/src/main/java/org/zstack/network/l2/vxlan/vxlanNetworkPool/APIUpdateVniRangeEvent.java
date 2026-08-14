package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author: kefeng.wang
 * @date: 2018-12-12
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVniRangeEvent extends APIEvent {
    private VniRangeInventory inventory;

    public APIUpdateVniRangeEvent() {
    }

    public APIUpdateVniRangeEvent(String apiId) {
        super(apiId);
    }

    public VniRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VniRangeInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVniRangeEvent __example__() {
        APIUpdateVniRangeEvent event = new APIUpdateVniRangeEvent();

        VniRangeInventory inventory = new VniRangeInventory();
        inventory.setName("Test-Range");
        inventory.setDescription("Test");
        inventory.setL2NetworkUuid(uuid());
        inventory.setStartVni(10);
        inventory.setEndVni(10000);
        event.setInventory(inventory);

        return event;
    }
}
