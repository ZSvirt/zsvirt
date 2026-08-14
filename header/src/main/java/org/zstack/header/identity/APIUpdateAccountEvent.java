package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateAccountEvent extends APIEvent {
    private AccountInventory inventory;

    public APIUpdateAccountEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateAccountEvent() {
        super(null);
    }

    public AccountInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateAccountEvent __example__() {
        APIUpdateAccountEvent event = new APIUpdateAccountEvent();

        AccountInventory inventory = new AccountInventory();
        inventory.setName("test");
        inventory.setType(AccountType.Normal.toString());
        inventory.setUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
