package org.zstack.log.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateLogServerEvent extends APIEvent {
    private LogServerInventory inventory;

    public APIUpdateLogServerEvent() {
    }

    public APIUpdateLogServerEvent(String apiId) {
        super(apiId);
    }

    public LogServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LogServerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateLogServerEvent __example__() {
        APIUpdateLogServerEvent event = new APIUpdateLogServerEvent();
        return event;
    }
}
