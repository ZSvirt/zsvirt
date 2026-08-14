package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2018-12-26.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPreconfigurationTemplatesReply extends APIQueryReply {
    List<PreconfigurationTemplateInventory> inventories;

    public List<PreconfigurationTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PreconfigurationTemplateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPreconfigurationTemplatesReply __example__() {
        APIQueryPreconfigurationTemplatesReply reply = new APIQueryPreconfigurationTemplatesReply();
        reply.setInventories(Collections.singletonList(PreconfigurationTemplateInventory.__example__()));
        return reply;
    }
}
