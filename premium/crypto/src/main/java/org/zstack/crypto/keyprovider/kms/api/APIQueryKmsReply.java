package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.header.keyprovider.KmsInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryKmsReply extends APIQueryReply {
    private List<KmsInventory> inventories;

    public List<KmsInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<KmsInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryKmsReply __example__() {
        APIQueryKmsReply reply = new APIQueryKmsReply();
        reply.setInventories(java.util.Collections.singletonList(KmsInventory.__example__()));
        reply.setSuccess(true);
        return reply;
    }
}
