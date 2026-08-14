package org.zstack.crypto.keyprovider.api;

import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryKeyProviderReply extends APIQueryReply {
    private List<KeyProviderInventory> inventories;

    public List<KeyProviderInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<KeyProviderInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryKeyProviderReply __example__() {
        APIQueryKeyProviderReply reply = new APIQueryKeyProviderReply();
        reply.setInventories(Collections.singletonList(KeyProviderInventory.__example__()));
        return reply;
    }
}
