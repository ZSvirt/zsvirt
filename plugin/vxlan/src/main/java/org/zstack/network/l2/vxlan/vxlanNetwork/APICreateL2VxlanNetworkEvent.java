package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.network.l2.APICreateL2NetworkEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateL2VxlanNetworkEvent extends APICreateL2NetworkEvent {
    public APICreateL2VxlanNetworkEvent(String apiId) {
        super(apiId);
    }

    public APICreateL2VxlanNetworkEvent() {
        super(null);
    }

    public static APICreateL2VxlanNetworkEvent __example__() {
        APICreateL2VxlanNetworkEvent event = new APICreateL2VxlanNetworkEvent();
        L2VxlanNetworkInventory net = new L2VxlanNetworkInventory();

        net.setName("Test-Net");
        net.setVni(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPoolUuid(uuid());
        net.setType("L2VxlanNetwork");

        event.setInventory(net);
        return event;
    }
}
