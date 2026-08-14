package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 4/26/17.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateBaremetalChassisEvent extends APIEvent {
    BaremetalChassisInventory inventory;

    public BaremetalChassisInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalChassisInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateBaremetalChassisEvent() {
    }

    public APIUpdateBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateBaremetalChassisEvent __example__() {
        APIUpdateBaremetalChassisEvent evt = new APIUpdateBaremetalChassisEvent();
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
