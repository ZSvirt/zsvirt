package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2017/3/27.
 */
@RestResponse(allTo = "inventory")
public class APICreateBaremetalPxeServerEvent extends APIEvent {
    BaremetalPxeServerInventory inventory;

    public BaremetalPxeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalPxeServerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateBaremetalPxeServerEvent() {
        super(null);
    }

    public APICreateBaremetalPxeServerEvent(String apiId) {
        super(apiId);
    }

    public static APICreateBaremetalPxeServerEvent __example__() {
        APICreateBaremetalPxeServerEvent evt = new APICreateBaremetalPxeServerEvent();
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
        inv.setStatus(BaremetalPxeServerStatus.Connecting.toString());
        evt.setInventory(inv);
        return evt;
    }
}
