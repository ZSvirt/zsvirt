package org.zstack.header.console;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:27 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventory")
public class APIRequestConsoleAccessEvent extends APIEvent {
    private ConsoleInventory inventory;

    public APIRequestConsoleAccessEvent() {
        super(null);
    }

    public APIRequestConsoleAccessEvent(String apiId) {
        super(apiId);
    }

    public ConsoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(ConsoleInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRequestConsoleAccessEvent __example__() {
        APIRequestConsoleAccessEvent event = new APIRequestConsoleAccessEvent();
        ConsoleInventory inventory = new ConsoleInventory();
        inventory.setHostname("127.0.0.1");
        inventory.setPort(4900);
        inventory.setScheme("http");
        inventory.setToken(uuid());
        inventory.setVersion("1.0.0");

        event.setInventory(inventory);
        return event;
    }

}
