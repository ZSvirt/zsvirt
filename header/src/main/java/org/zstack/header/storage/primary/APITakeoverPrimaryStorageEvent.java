package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

@RestResponse(fieldsTo = {"all"})
public class APITakeoverPrimaryStorageEvent extends APIEvent {
    private PrimaryStorageInventory inventory;

    public APITakeoverPrimaryStorageEvent() {
    }

    public APITakeoverPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public static APITakeoverPrimaryStorageEvent __example__() {
        APITakeoverPrimaryStorageEvent event = new APITakeoverPrimaryStorageEvent();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("SharedBlock");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));

        event.setInventory(ps);
        return event;
    }
}
