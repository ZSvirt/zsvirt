package org.zstack.iam1.api.accounts;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryAccountGroupReply extends APIQueryReply {
    private List<AccountGroupInventory> inventories;

    public List<AccountGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAccountGroupReply __example__() {
        APIQueryAccountGroupReply event = new APIQueryAccountGroupReply();
        event.setInventories(list(AccountGroupInventory.__example__()));
        return event;
    }
}
