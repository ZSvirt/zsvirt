package org.zstack.header.longjob;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 11/13/17.
 */
@RestResponse(allTo = "inventories")
public class APIQueryLongJobReply extends APIQueryReply {
    private List<LongJobInventory> inventories;

    public List<LongJobInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LongJobInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryLongJobReply __example__() {
        APIQueryLongJobReply reply = new APIQueryLongJobReply();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
