package org.zstack.imagereplicator;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryImageReplicationGroupReply extends APIQueryReply {
    private List<ImageReplicationGroupInventory> inventories;

    public List<ImageReplicationGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageReplicationGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryImageReplicationGroupReply __example__() {
        APIQueryImageReplicationGroupReply reply = new APIQueryImageReplicationGroupReply();
        ImageReplicationGroupInventory inventory = new ImageReplicationGroupInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        reply.setInventories(Collections.singletonList(inventory));
        return reply;
    }
}
