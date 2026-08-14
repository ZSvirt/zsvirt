package org.zstack.header.console;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateConsoleProxyAgentEvent extends APIEvent {
    private ConsoleProxyAgentInventory inventory;

    public APIUpdateConsoleProxyAgentEvent() {
    }

    public APIUpdateConsoleProxyAgentEvent(String apiId) {
        super(apiId);
    }

    public ConsoleProxyAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(ConsoleProxyAgentInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateConsoleProxyAgentEvent __example__() {
        APIUpdateConsoleProxyAgentEvent event = new APIUpdateConsoleProxyAgentEvent();
        ConsoleProxyAgentInventory inventory = new ConsoleProxyAgentInventory();
        event.setInventory(inventory);
        return event;
    }
}
