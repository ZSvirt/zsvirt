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

        AccountInventory inventory = new AccountInventory();
        inventory.setName("test");
        inventory.setType(AccountType.SystemAdmin.toString());
        inventory.setUuid(uuid());

        event.setInventory(inventory);
        return event;
    }
}
