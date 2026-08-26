package org.zstack.log.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddLogServerEvent extends APIEvent {
    private LogServerInventory inventory;

    public APIAddLogServerEvent() {
    }

    public APIAddLogServerEvent(String apiId) {
        super(apiId);
    }

    public LogServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LogServerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddLogServerEvent __example__() {
        APIAddLogServerEvent event = new APIAddLogServerEvent();
        LogServerInventory inventory = new LogServerInventory();
        event.setInventory(inventory);
        return event;
    }
}
