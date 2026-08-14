package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;

@RestResponse(allTo = "inventory")
public class APICreateAccountGroupEvent extends APIEvent {
    private AccountGroupInventory inventory;

    public APICreateAccountGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreateAccountGroupEvent() {
        super(null);
    }

    public AccountGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateAccountGroupEvent __example__() {
        APICreateAccountGroupEvent event = new APICreateAccountGroupEvent();
        event.setInventory(AccountGroupInventory.__example__());
        return event;
    }
}
