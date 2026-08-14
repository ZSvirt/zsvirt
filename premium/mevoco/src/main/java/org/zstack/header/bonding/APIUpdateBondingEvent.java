package org.zstack.header.bonding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateBondingEvent extends APIEvent {
    private HostNetworkBondingInventory inventory;

    public APIUpdateBondingEvent() {
    }

    public APIUpdateBondingEvent(String apiId) { super(apiId); }

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateBondingEvent __example__() {
        APIUpdateBondingEvent evt = new APIUpdateBondingEvent();
        evt.setSuccess(true);
        return evt;
    }
}
