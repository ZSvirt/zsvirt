package org.zstack.header.bonding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

@RestResponse(allTo = "inventory")
public class APIDetachNicFromBondingEvent extends APIEvent {
    private HostNetworkBondingInventory inventory;

    public APIDetachNicFromBondingEvent() {
    }

    public APIDetachNicFromBondingEvent(String apiId) { super(apiId); }

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }

    public static APIDetachNicFromBondingEvent __example__() {
        APIDetachNicFromBondingEvent evt = new APIDetachNicFromBondingEvent();
        evt.setSuccess(true);
        return evt;
    }
}
