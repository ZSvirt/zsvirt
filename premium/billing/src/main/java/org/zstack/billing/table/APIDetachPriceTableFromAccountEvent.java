package org.zstack.billing.table;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/9/10.
 */
@RestResponse(allTo = "inventory")
public class APIDetachPriceTableFromAccountEvent extends APIEvent {
    private PriceTableInventory inventory;

    public APIDetachPriceTableFromAccountEvent() {
    }

    public APIDetachPriceTableFromAccountEvent(String apiId) {
        super(apiId);
    }

    public PriceTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceTableInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachPriceTableFromAccountEvent __example__() {
        APIDetachPriceTableFromAccountEvent event = new APIDetachPriceTableFromAccountEvent();
        PriceTableInventory inventory = new PriceTableInventory();
        inventory.setUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
