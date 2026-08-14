package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestResponse(allTo = "inventory")
public class APIInspectBaremetalChassisEvent extends APIEvent {
    private BaremetalChassisInventory inventory;

    public BaremetalChassisInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalChassisInventory inventory) {
        this.inventory = inventory;
    }

    public APIInspectBaremetalChassisEvent() {
        super(null);
    }

    public APIInspectBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIInspectBaremetalChassisEvent __example__() {
        APIInspectBaremetalChassisEvent evt = new APIInspectBaremetalChassisEvent();
        BaremetalChassisInventory inv = new BaremetalChassisInventory();
        inv.setUuid(uuid());
        inv.setIpmiAddress("1.1.1.1");
        inv.setIpmiPort(623);
        inv.setIpmiUsername("root");
        inv.setIpmiPassword("password");
        inv.setState(BaremetalChassisState.Enabled.toString());
        inv.setStatus(BaremetalChassisStatus.PxeBooting.toString());
        evt.setInventory(inv);
        return evt;
    }
}
