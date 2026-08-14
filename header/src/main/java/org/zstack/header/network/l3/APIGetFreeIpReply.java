package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventories")
public class APIGetFreeIpReply extends APIReply {
    private List<FreeIpInventory> inventories;

    public List<FreeIpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FreeIpInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetFreeIpReply __example__() {
        APIGetFreeIpReply reply = new APIGetFreeIpReply();

        String iprUuid = uuid();

        List<FreeIpInventory> invs = new ArrayList<>();

        FreeIpInventory inv = new FreeIpInventory();
        inv.setNetmask("255.255.255.0");
        inv.setIp("10.20.10.5");
        inv.setGateway("10.20.10.1");
        inv.setIpRangeUuid(iprUuid);
        invs.add(inv);

        inv = new FreeIpInventory();
        inv.setNetmask("255.255.255.0");
        inv.setIp("10.20.10.6");
        inv.setGateway("10.20.10.1");
        inv.setIpRangeUuid(iprUuid);
        invs.add(inv);

        inv = new FreeIpInventory();
        inv.setNetmask("255.255.255.0");
        inv.setIp("10.20.10.10");
        inv.setGateway("10.20.10.1");
        inv.setIpRangeUuid(iprUuid);
        invs.add(inv);

        reply.setInventories(invs);

        return reply;
    }
}
