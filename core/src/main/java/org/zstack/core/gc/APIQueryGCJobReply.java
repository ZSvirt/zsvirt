package org.zstack.core.gc;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestResponse(allTo = "inventories")
public class APIQueryGCJobReply extends APIQueryReply {
    private List<GarbageCollectorInventory> inventories;

    public List<GarbageCollectorInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GarbageCollectorInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGCJobReply __example__() {
        APIQueryGCJobReply reply = new APIQueryGCJobReply();
        reply.setInventories(list(GarbageCollectorInventory.__example__()));
        return reply;
    }
}
