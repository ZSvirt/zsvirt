package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/4/16.
 */
@RestResponse(allTo = "inventory")
public class APIChangeResourceOwnerEvent extends APIEvent {
    private AccountResourceRefInventory inventory;

    public APIChangeResourceOwnerEvent() {
    }

    public APIChangeResourceOwnerEvent(String apiId) {
        super(apiId);
    }

    public AccountResourceRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountResourceRefInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeResourceOwnerEvent __example__() {
        APIChangeResourceOwnerEvent event = new APIChangeResourceOwnerEvent();
        AccountResourceRefInventory inventory = new AccountResourceRefInventory();
        inventory.setAccountUuid(uuid());
        inventory.setResourceUuid(uuid());
        inventory.setResourceType("ImageVO");
        inventory.setId(1);
        inventory.setType("Own");

        event.setInventory(inventory);
        return event;
    }

}
