package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpInventory;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryHostNetworkInterfaceLldpReply extends APIQueryReply {
    private List<HostNetworkInterfaceLldpInventory> inventories;

    public List<HostNetworkInterfaceLldpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostNetworkInterfaceLldpInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostNetworkInterfaceLldpReply __example__() {
        APIQueryHostNetworkInterfaceLldpReply reply = new APIQueryHostNetworkInterfaceLldpReply();
        HostNetworkInterfaceLldpInventory inv = new HostNetworkInterfaceLldpInventory();
        inv.setInterfaceUuid(uuid());
        inv.setMode("rx_only");

        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}