package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIResumeLongJobEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APIResumeLongJobEvent() {
        super();
    }

    public APIResumeLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APIResumeLongJobEvent __example__() {
        APIResumeLongJobEvent event = new APIResumeLongJobEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
