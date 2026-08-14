package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/6/18.
 */
@RestResponse(allTo = "inventory")
public class APIChangeBaremetalChassisStateEvent extends APIEvent {
    private BaremetalChassisInventory inventory;

    public APIChangeBaremetalChassisStateEvent() {
    }

    public APIChangeBaremetalChassisStateEvent(String apiId) {
        super(apiId);
    }

    public BaremetalChassisInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalChassisInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeBaremetalChassisStateEvent __example__() {
        APIChangeBaremetalChassisStateEvent evt = new APIChangeBaremetalChassisStateEvent();
        BaremetalChassisInventory inv = new BaremetalChassisInventory();
        inv.setUuid(uuid());
        inv.setIpmiAddress("1.1.1.1");
        inv.setIpmiPort(623);
        inv.setIpmiUsername("root");
        inv.setIpmiPassword("password");
        inv.setState(BaremetalChassisState.Enabled.toString());
        inv.setStatus(BaremetalChassisStatus.Available.toString());
        evt.setInventory(inv);
        return evt;
    }
}
