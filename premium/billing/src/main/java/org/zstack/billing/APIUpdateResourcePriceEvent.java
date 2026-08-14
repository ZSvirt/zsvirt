package org.zstack.billing;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by lining on 2019/10/24.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateResourcePriceEvent extends APIEvent {
    private PriceInventory inventory;

    public APIUpdateResourcePriceEvent() {
    }

    public APIUpdateResourcePriceEvent(String apiId) {
        super(apiId);
    }

    public PriceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateResourcePriceEvent __example__() {
        APIUpdateResourcePriceEvent event = new APIUpdateResourcePriceEvent();
        PriceInventory inventory = new PriceInventory();
        inventory.setUuid(uuid());
        inventory.setPrice(2d);
        inventory.setTimeUnit("s");
        inventory.setResourceName("Volume");
        inventory.setResourceUnit("1");
        inventory.setDateInLong(org.zstack.header.message.DocUtils.date);
        inventory.setEndDateInLong(org.zstack.header.message.DocUtils.date + 360000000);

        event.setInventory(inventory);
        return event;
    }

}
