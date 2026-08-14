package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateAccountEvent extends APIEvent {
    private AccountInventory inventory;

    public APICreateAccountEvent(String apiId) {
        super(apiId);
    }

    public APICreateAccountEvent() {
        super(null);
    }

    public AccountInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateAccountEvent __example__() {
        APICreateAccountEvent event = new APICreateAccountEvent();

        AccountInventory inventory = new AccountInventory();
        inventory.setName("test");
        inventory.setType(AccountType.Normal.toString());
        inventory.setUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
