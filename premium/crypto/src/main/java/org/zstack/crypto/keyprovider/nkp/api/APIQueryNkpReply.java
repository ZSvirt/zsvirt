package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.header.keyprovider.NkpInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryNkpReply extends APIQueryReply {
    private List<NkpInventory> inventories;

    public List<NkpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NkpInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryNkpReply __example__() {
        APIQueryNkpReply reply = new APIQueryNkpReply();
        reply.setInventories(asList(NkpInventory.__example__()));
        reply.setSuccess(true);
        return reply;
    }
}
