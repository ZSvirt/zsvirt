package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 5/9/17.
 */
@RestResponse(allTo = "inventory")
public class APIStopBaremetalPxeServerEvent extends APIEvent {
    private BaremetalPxeServerInventory inventory;

    public APIStopBaremetalPxeServerEvent() {
    }

    public APIStopBaremetalPxeServerEvent(String apiId) {
        super(apiId);
    }

    public BaremetalPxeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalPxeServerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIStopBaremetalPxeServerEvent __example__() {
        APIStopBaremetalPxeServerEvent evt = new APIStopBaremetalPxeServerEvent();
        BaremetalPxeServerInventory inv = new BaremetalPxeServerInventory();
        inv.setUuid(uuid());
        inv.setName("test");
        inv.setHostname("127.0.0.1");
        inv.setSshUsername("root");
        inv.setSshPassword("password");
        inv.setSshPort(22);
        inv.setStoragePath("/zstack_bm_cache");
        inv.setDhcpInterface("eth0");
        inv.setDhcpRangeBegin("10.0.0.1");
        inv.setDhcpRangeEnd("10.0.0.255");
        inv.setDhcpRangeNetmask("255.255.255.0");
        inv.setState(BaremetalPxeServerState.Enabled.toString());
        inv.setStatus(BaremetalPxeServerStatus.Disconnected.toString());
        evt.setInventory(inv);
        return evt;
    }
}
