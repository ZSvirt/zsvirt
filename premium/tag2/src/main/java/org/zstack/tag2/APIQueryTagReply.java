package org.zstack.tag2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tag.TagPatternInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryTagReply extends APIQueryReply {
    private List<TagPatternInventory> inventories;

    public List<TagPatternInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TagPatternInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryTagReply __example__() {
        APIQueryTagReply reply = new APIQueryTagReply();
        reply.setInventories(list(TagPatternInventory.__example__()));
        return reply;
    }
}

