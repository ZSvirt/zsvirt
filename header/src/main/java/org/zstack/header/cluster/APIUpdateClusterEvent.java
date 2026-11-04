package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateClusterEvent extends APIEvent {
    private ClusterInventory inventory;

    public APIUpdateClusterEvent() {
    }

    public APIUpdateClusterEvent(String apiId) {
        super(apiId);
    }

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateClusterEvent __example__() {
        APIUpdateClusterEvent event = new APIUpdateClusterEvent();
        event.setInventory(ClusterInventory.__example__());
        return event;
    }
}
