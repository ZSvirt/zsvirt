package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIRenewSessionEvent extends APIEvent {
    private SessionInventory inventory;

    public APIRenewSessionEvent(String apiId) {
        super(apiId);
    }

    public APIRenewSessionEvent() {
        super(null);
    }

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRenewSessionEvent __example__() {
        APIRenewSessionEvent event = new APIRenewSessionEvent();

        SessionInventory inventory = new SessionInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setExpiredDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(inventory);
        return event;
    }

}