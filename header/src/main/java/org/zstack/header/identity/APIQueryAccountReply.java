package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryAccountReply extends APIQueryReply {
    private List<AccountInventory> inventories;

    public List<AccountInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAccountReply __example__() {
        APIQueryAccountReply reply = new APIQueryAccountReply();
        AccountInventory inventory = new AccountInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        inventory.setType(AccountType.Normal.toString());
        reply.setInventories(list(inventory));
        return reply;
    }

}
