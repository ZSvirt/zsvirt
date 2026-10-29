package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeAccountTypeEvent extends APIEvent {
    private AccountInventory inventory;

    public APIChangeAccountTypeEvent(String apiId) {
        super(apiId);
    }

    public APIChangeAccountTypeEvent() {
        super(null);
    }

    public AccountInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeAccountTypeEvent __example__() {
        APIChangeAccountTypeEvent event = new APIChangeAccountTypeEvent();
        event.setInventory(AccountInventory.__example__());
        event.getInventory().setType(AccountType.SystemAdmin.toString());
        return event;
    }
}
