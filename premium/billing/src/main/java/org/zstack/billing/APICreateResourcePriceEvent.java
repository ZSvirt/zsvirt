package org.zstack.billing;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 2/23/2016.
 */
@RestResponse(allTo = "inventory")
public class APICreateResourcePriceEvent extends APIEvent {
    private PriceInventory inventory;

    public APICreateResourcePriceEvent() {
    }

    public APICreateResourcePriceEvent(String apiId) {
        super(apiId);
    }

    public PriceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateResourcePriceEvent __example__() {
        APICreateResourcePriceEvent event = new APICreateResourcePriceEvent();
        PriceInventory inventory = new PriceInventory();
        inventory.setUuid(uuid());
        inventory.setPrice(2d);
        inventory.setTimeUnit("s");
        inventory.setResourceName("Volume");
        inventory.setResourceUnit("1");

        event.setInventory(inventory);
        return event;
    }

}
