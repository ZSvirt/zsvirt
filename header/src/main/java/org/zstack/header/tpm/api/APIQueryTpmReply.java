package org.zstack.header.tpm.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tpm.entity.TpmInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryTpmReply extends APIQueryReply {
    private List<TpmInventory> inventories;

    public List<TpmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TpmInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryTpmReply __example__() {
        APIQueryTpmReply reply = new APIQueryTpmReply();
        reply.setInventories(list(TpmInventory.__example__()));
        return reply;
    }
}
