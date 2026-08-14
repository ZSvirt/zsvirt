package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAttachL2NetworkToHostEvent extends APIEvent {
    /**
     * @desc see :ref:`L2NetworkInventory`
     */
    private L2NetworkInventory inventory;

    public APIAttachL2NetworkToHostEvent(String apiId) {
        super(apiId);
    }

    public APIAttachL2NetworkToHostEvent() {
        super(null);
    }

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachL2NetworkToHostEvent __example__() {
        APIAttachL2NetworkToHostEvent event = new APIAttachL2NetworkToHostEvent();
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