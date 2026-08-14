package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.l2.vxlan.vtep.RemoteVtepInventory;

@RestResponse(allTo = "inventory")
public class APICreateVxlanPoolRemoteVtepEvent extends APIEvent {

    private RemoteVtepInventory inventory;

    public APICreateVxlanPoolRemoteVtepEvent(String apiId) {
        super(apiId);
    }

    public APICreateVxlanPoolRemoteVtepEvent() {
        super(null);
    }

    public RemoteVtepInventory getInventory() {
        return inventory;
    }

    public void setInventory(RemoteVtepInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateVxlanPoolRemoteVtepEvent __example__() {
        APICreateVxlanPoolRemoteVtepEvent event = new APICreateVxlanPoolRemoteVtepEvent();
        RemoteVtepInventory vtep = new RemoteVtepInventory();

        vtep.setUuid(uuid());
        vtep.setVtepIp("10.10.1.1");
        vtep.setPort(8472);
        vtep.setPoolUuid(uuid());
        vtep.setType("KVM_HOST_VXLAN");

        event.setInventory(vtep);
        return event;
    }

}
