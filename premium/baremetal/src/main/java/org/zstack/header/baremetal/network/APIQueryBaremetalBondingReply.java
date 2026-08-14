package org.zstack.header.baremetal.network;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-01-03.
 */
@RestResponse(allTo = "inventories")
public class APIQueryBaremetalBondingReply extends APIQueryReply {
    List<BaremetalBondingInventory> inventories;

    public List<BaremetalBondingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BaremetalBondingInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryBaremetalBondingReply __example__() {
        APIQueryBaremetalBondingReply reply = new APIQueryBaremetalBondingReply();
        reply.setInventories(Collections.singletonList(BaremetalBondingInventory.__example__()));
        return reply;
    }
}
