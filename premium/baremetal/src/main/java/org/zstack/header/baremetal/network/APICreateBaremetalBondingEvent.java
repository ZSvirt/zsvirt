package org.zstack.header.baremetal.network;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-01-03.
 */
@RestResponse(allTo = "inventory")
public class APICreateBaremetalBondingEvent extends APIEvent {
    private BaremetalBondingInventory inventory;

    public BaremetalBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalBondingInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateBaremetalBondingEvent() {
    }

    public APICreateBaremetalBondingEvent(String apiId) {
        super(apiId);
    }

    public static APICreateBaremetalBondingEvent __example__() {
        APICreateBaremetalBondingEvent evt = new APICreateBaremetalBondingEvent();
        evt.setInventory(BaremetalBondingInventory.__example__());
        return evt;
    }
}
