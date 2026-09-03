package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateAccountGroupEvent extends APIEvent {
    private AccountGroupInventory inventory;

    public APIUpdateAccountGroupEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateAccountGroupEvent() {
        super(null);
    }

    public AccountGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateAccountGroupEvent __example__() {
        APIUpdateAccountGroupEvent event = new APIUpdateAccountGroupEvent();
        event.setInventory(AccountGroupInventory.__example__());
        return event;
    }
}
