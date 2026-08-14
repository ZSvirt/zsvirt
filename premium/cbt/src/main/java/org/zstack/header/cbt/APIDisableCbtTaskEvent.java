package org.zstack.header.cbt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIDisableCbtTaskEvent extends APIEvent {
    private CbtTaskInventory inventory;

    public CbtTaskInventory getInventory() {
        return inventory;
    }

    public void setInventory(CbtTaskInventory inventory) {
        this.inventory = inventory;
    }

    public APIDisableCbtTaskEvent() {
    }

    public APIDisableCbtTaskEvent(String msgId) {
        super(msgId);
    }

    public static APIDisableCbtTaskEvent __example__() {
        APIDisableCbtTaskEvent event = new APIDisableCbtTaskEvent();

        CbtTaskInventory inv = new CbtTaskInventory();

        inv.setUuid(uuid());
        inv.setName("My Task");
        inv.setStatus(CbtTaskStatus.Stopped);

        event.setInventory(inv);
        return event;
    }
}
