package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 2/25/2016.
 */
@RestResponse(allTo = "inventories")
public class APIQueryAccountResourceRefReply extends APIQueryReply {
    private List<AccountResourceRefInventory> inventories;

    public List<AccountResourceRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountResourceRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAccountResourceRefReply __example__() {
        APIQueryAccountResourceRefReply reply = new APIQueryAccountResourceRefReply();
        AccountResourceRefInventory inventory = new AccountResourceRefInventory();
        inventory.setAccountUuid(uuid());
        inventory.setId(1);
        inventory.setResourceType("ImageVO");
        inventory.setResourceUuid(uuid());
        inventory.setType("Own");
        reply.setInventories(list(inventory));
        return reply;
    }

}
