package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 4/20/17.
 */
@RestResponse(allTo = "inventories")
public class APIQueryBaremetalPxeServerReply extends APIQueryReply {
    List<BaremetalPxeServerInventory> inventories;

    public List<BaremetalPxeServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BaremetalPxeServerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryBaremetalPxeServerReply __example__() {
        APIQueryBaremetalPxeServerReply reply = new APIQueryBaremetalPxeServerReply();
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
        inv.setStatus(BaremetalPxeServerStatus.Connected.toString());
        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
