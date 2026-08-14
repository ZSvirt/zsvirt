package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by boce.wang on 03/20/2024.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateL2NetworkVirtualNetworkIdEvent extends APIEvent {
    private L2NetworkInventory inventory;

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateL2NetworkVirtualNetworkIdEvent() {
        super();
    }

    public APIUpdateL2NetworkVirtualNetworkIdEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateL2NetworkVirtualNetworkIdEvent __example__() {
        APIUpdateL2NetworkVirtualNetworkIdEvent event = new APIUpdateL2NetworkVirtualNetworkIdEvent();
        L2VlanNetworkInventory net = new L2VlanNetworkInventory();

        net.setName("Test-Net");
        net.setVlan(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0");
        net.setType("L2VlanNetwork");

        event.setInventory(net);
        return event;
    }
}
