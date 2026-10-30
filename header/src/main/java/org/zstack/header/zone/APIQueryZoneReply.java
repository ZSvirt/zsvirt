package org.zstack.header.zone;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryZoneReply extends APIQueryReply {
    private List<ZoneInventory> inventories;

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryZoneReply __example__() {
        APIQueryZoneReply reply = new APIQueryZoneReply();
        reply.setInventories(list(ZoneInventory.__example__()));
        return reply;
    }
}
