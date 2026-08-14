package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 4/26/17.
 */
@RestResponse(allTo = "inventory")
public class APICreateBaremetalChassisEvent extends APIEvent {
    private BaremetalChassisInventory inventory;

    public BaremetalChassisInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalChassisInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateBaremetalChassisEvent() {
    }

    public APICreateBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APICreateBaremetalChassisEvent __example__() {
        APICreateBaremetalChassisEvent evt = new APICreateBaremetalChassisEvent();
        BaremetalChassisInventory inv = new BaremetalChassisInventory();
        inv.setUuid(uuid());
        inv.setIpmiAddress("1.1.1.1");
        inv.setIpmiPort(623);
        inv.setIpmiUsername("root");
        inv.setIpmiPassword("password");
        inv.setState(BaremetalChassisState.Enabled.toString());
        inv.setStatus(BaremetalChassisStatus.HWInfoUnknown.toString());
        evt.setInventory(inv);
        return evt;
    }
}
