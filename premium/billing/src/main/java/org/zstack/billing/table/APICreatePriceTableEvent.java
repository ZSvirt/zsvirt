package org.zstack.billing.table;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by lining on 2019/9/10.
 */
@RestResponse(allTo = "inventory")
public class APICreatePriceTableEvent extends APIEvent {
    private PriceTableInventory inventory;

    public APICreatePriceTableEvent() {
    }

    public APICreatePriceTableEvent(String apiId) {
        super(apiId);
    }

    public PriceTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceTableInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreatePriceTableEvent __example__() {
        APICreatePriceTableEvent event = new APICreatePriceTableEvent();
        PriceTableInventory inventory = new PriceTableInventory();
        inventory.setUuid(uuid());
        inventory.setName("price table");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inventory);
        return event;
    }

}
