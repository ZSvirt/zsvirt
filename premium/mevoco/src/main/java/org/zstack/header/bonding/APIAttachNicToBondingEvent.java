package org.zstack.header.bonding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

@RestResponse(allTo = "inventory")
public class APIAttachNicToBondingEvent extends APIEvent {
    private HostNetworkBondingInventory inventory;

    public APIAttachNicToBondingEvent() {
    }

    public APIAttachNicToBondingEvent(String apiId) { super(apiId); }

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachNicToBondingEvent __example__() {
        APIAttachNicToBondingEvent evt = new APIAttachNicToBondingEvent();
        evt.setSuccess(true);
        return evt;
    }
}
