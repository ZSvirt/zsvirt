package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupView;

@RestResponse(allTo = "inventory")
public class APIGetAccountGroupTreeReply extends APIReply {
    private AccountGroupView inventory;

    public AccountGroupView getInventory() {
        return inventory;
    }

    public void setInventory(AccountGroupView inventory) {
        this.inventory = inventory;
    }

    public static APIGetAccountGroupTreeReply __example__() {
        APIGetAccountGroupTreeReply reply = new APIGetAccountGroupTreeReply();
        reply.setInventory(AccountGroupView.__example__());
        return reply;
    }
}
