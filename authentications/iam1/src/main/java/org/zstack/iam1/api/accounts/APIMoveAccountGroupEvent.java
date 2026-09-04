package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;

@RestResponse(allTo = "inventory")
public class APIMoveAccountGroupEvent extends APIEvent {
    private AccountGroupInventory inventory;

    public APIMoveAccountGroupEvent(String apiId) {
        super(apiId);
    }

    public APIMoveAccountGroupEvent() {
        super(null);
    }

    public AccountGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIMoveAccountGroupEvent __example__() {
        APIMoveAccountGroupEvent event = new APIMoveAccountGroupEvent();
        event.setInventory(AccountGroupInventory.__example__());
        return event;
    }
}
