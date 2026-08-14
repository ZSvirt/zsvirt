package org.zstack.header.cbt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateCbtTaskEvent extends APIEvent {
    private CbtTaskInventory inventory;

    public APICreateCbtTaskEvent() {
    }

    public APICreateCbtTaskEvent(String msgId) {
        super(msgId);
    }

    public CbtTaskInventory getInventory() {
        return inventory;
    }

    public void setInventory(CbtTaskInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateCbtTaskEvent __example__() {
        APICreateCbtTaskEvent event = new APICreateCbtTaskEvent();
        CbtTaskInventory inv = new CbtTaskInventory();

        inv.setUuid(uuid());
        inv.setName("My Task");
        inv.setStatus(CbtTaskStatus.Created);

        event.setInventory(inv);
        return event;
    }
}
