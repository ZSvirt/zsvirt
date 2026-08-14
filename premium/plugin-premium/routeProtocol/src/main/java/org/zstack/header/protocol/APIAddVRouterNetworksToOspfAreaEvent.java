package org.zstack.header.protocol;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIAddVRouterNetworksToOspfAreaEvent extends APIEvent {
    private List<NetworkRouterAreaRefInventory> inventories;

    public APIAddVRouterNetworksToOspfAreaEvent() {
        super(null);
    }

    public APIAddVRouterNetworksToOspfAreaEvent(String apiId) {
        super(apiId);
    }

    public List<NetworkRouterAreaRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkRouterAreaRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIAddVRouterNetworksToOspfAreaEvent __example__() {
        APIAddVRouterNetworksToOspfAreaEvent event = new APIAddVRouterNetworksToOspfAreaEvent();

        NetworkRouterAreaRefInventory routerAreaRef = new NetworkRouterAreaRefInventory();
        routerAreaRef.setUuid(uuid());
        routerAreaRef.setvRouterUuid(uuid());
        routerAreaRef.setRouterAreaUuid(uuid());
        routerAreaRef.setL3NetworkUuid(uuid());
        routerAreaRef.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        routerAreaRef.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventories(asList(routerAreaRef));

        return event;
    }

}
