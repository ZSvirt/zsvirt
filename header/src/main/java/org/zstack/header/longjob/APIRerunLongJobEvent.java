package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2018-09-11.
 */
@RestResponse(allTo = "inventory")
public class APIRerunLongJobEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APIRerunLongJobEvent() {
    }

    public APIRerunLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APIRerunLongJobEvent __example__() {
        APIRerunLongJobEvent event = new APIRerunLongJobEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
