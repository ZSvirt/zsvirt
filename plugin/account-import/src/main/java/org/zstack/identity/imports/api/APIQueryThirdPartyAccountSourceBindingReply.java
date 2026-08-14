package org.zstack.identity.imports.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryThirdPartyAccountSourceBindingReply extends APIQueryReply {
    private List<AccountThirdPartyAccountSourceRefInventory> inventories;

    public List<AccountThirdPartyAccountSourceRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountThirdPartyAccountSourceRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryThirdPartyAccountSourceBindingReply __example__() {
        APIQueryThirdPartyAccountSourceBindingReply reply = new APIQueryThirdPartyAccountSourceBindingReply();

        AccountThirdPartyAccountSourceRefInventory inventory = new AccountThirdPartyAccountSourceRefInventory();
        inventory.setId(1L);
        inventory.setCredentials("ou=Employee,uid=test");
        inventory.setAccountUuid(uuid());
        inventory.setAccountSourceUuid(uuid());

        reply.setInventories(list(inventory));
        return reply;
    }

}
