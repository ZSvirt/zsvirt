package org.zstack.header.zone;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIGetZoneReply extends APIReply {
    private List<ZoneInventory> inventories;

    public List<ZoneInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetZoneReply __example__() {
        APIGetZoneReply reply = new APIGetZoneReply();
        reply.setInventories(list(ZoneInventory.__example__()));
        reply.setSuccess(true);
        return reply;
    }

}
