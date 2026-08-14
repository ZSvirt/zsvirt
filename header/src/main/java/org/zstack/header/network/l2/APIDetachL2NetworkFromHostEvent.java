package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIDetachL2NetworkFromHostEvent extends APIEvent {
    /**
     * @desc see :ref:`L2NetworkInventory`
     */
    private L2NetworkInventory inventory;

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }

    public APIDetachL2NetworkFromHostEvent(String apiId) {
        super(apiId);
    }

    public APIDetachL2NetworkFromHostEvent() {
        super(null);
    }

    public static APIDetachL2NetworkFromHostEvent __example__() {
        APIDetachL2NetworkFromHostEvent event = new APIDetachL2NetworkFromHostEvent();
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