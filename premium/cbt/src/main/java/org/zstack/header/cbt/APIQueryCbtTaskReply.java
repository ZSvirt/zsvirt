package org.zstack.header.cbt;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryCbtTaskReply extends APIQueryReply {
    private List<CbtTaskInventory> inventories;

    public List<CbtTaskInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<CbtTaskInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryCbtTaskReply __example__() {
        APIQueryCbtTaskReply reply = new APIQueryCbtTaskReply();
        CbtTaskInventory inv = new CbtTaskInventory();

        inv.setUuid(uuid());
        inv.setName("My Task");
        inv.setStatus(CbtTaskStatus.Running);
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}
