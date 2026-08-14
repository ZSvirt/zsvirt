package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

@RestResponse(allTo = "inventory")
public class APICreateVxlanVtepEvent extends APIEvent {
    /**
     * @desc see :ref:`L2VlanNetworkInventory`
     */
    private VtepInventory inventory;

    public APICreateVxlanVtepEvent(String apiId) {
        super(apiId);
    }

    public APICreateVxlanVtepEvent() {
        super(null);
    }

    public VtepInventory getInventory() {
        return inventory;
    }

    public void setInventory(VtepInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVxlanVtepEvent __example__() {
        APICreateVxlanVtepEvent event = new APICreateVxlanVtepEvent();
        VtepInventory vtep = new VtepInventory();

        vtep.setHostUuid(uuid());
        vtep.setPoolUuid(uuid());
        vtep.setUuid(uuid());
        vtep.setVtepIp("1.1.1.1");

        event.setInventory(vtep);
        return event;
    }
}
