package org.zstack.header.console;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/15.
 */
@RestResponse(allTo = "inventory")
public class APIReconnectConsoleProxyAgentEvent extends APIEvent {
    @NoJsonSchema
    private Map<String, Object> inventory;

    public APIReconnectConsoleProxyAgentEvent() {
    }

    public APIReconnectConsoleProxyAgentEvent(String apiId) {
        super(apiId);
    }


    public Map<String, Object> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, Object> inventory) {
        this.inventory = inventory;
    }
 
    public static APIReconnectConsoleProxyAgentEvent __example__() {
        APIReconnectConsoleProxyAgentEvent event = new APIReconnectConsoleProxyAgentEvent();
        Map<String, Object> inventory = new HashMap<>();
        inventory.put(uuid(), true);
        inventory.put(uuid(), true);

        event.setInventory(inventory);
        return event;
    }

}
