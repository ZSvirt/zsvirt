package org.zstack.billing.table;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/9/28.
 */
@RestResponse(allTo = "inventory")
public class APIChangeAccountPriceTableBindingEvent extends APIEvent {
    private PriceTableInventory inventory;

    public APIChangeAccountPriceTableBindingEvent() {
    }

    public APIChangeAccountPriceTableBindingEvent(String apiId) {
        super(apiId);
    }

    public PriceTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceTableInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeAccountPriceTableBindingEvent __example__() {
        APIChangeAccountPriceTableBindingEvent event = new APIChangeAccountPriceTableBindingEvent();
        PriceTableInventory inventory = new PriceTableInventory();
        inventory.setUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
