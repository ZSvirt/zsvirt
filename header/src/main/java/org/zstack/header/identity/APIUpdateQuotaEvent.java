package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateQuotaEvent extends APIEvent {
    private QuotaInventory inventory;

    public APIUpdateQuotaEvent() {
    }

    public APIUpdateQuotaEvent(String apiId) {
        super(apiId);
    }

    public QuotaInventory getInventory() {
        return inventory;
    }

    public void setInventory(QuotaInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateQuotaEvent __example__() {
        APIUpdateQuotaEvent event = new APIUpdateQuotaEvent();
        QuotaInventory inventory = new QuotaInventory();
        inventory.setName("quota");
        inventory.setValue(20);
        inventory.setIdentityUuid(uuid());
        event.setInventory(inventory);
        return event;
    }

}
