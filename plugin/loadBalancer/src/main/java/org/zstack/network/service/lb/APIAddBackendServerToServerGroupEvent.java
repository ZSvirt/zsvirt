package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddBackendServerToServerGroupEvent extends APIEvent {
    private LoadBalancerServerGroupInventory inventory;

    public APIAddBackendServerToServerGroupEvent() {
    }

    public APIAddBackendServerToServerGroupEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerServerGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerServerGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddBackendServerToServerGroupEvent __example__() {
        APIAddBackendServerToServerGroupEvent event = new APIAddBackendServerToServerGroupEvent();
        LoadBalancerServerGroupInventory loadBalancerServerGroupIv = new LoadBalancerServerGroupInventory();
        loadBalancerServerGroupIv.setName("test-servergroup");
        loadBalancerServerGroupIv.setUuid(uuid());
        loadBalancerServerGroupIv.setLoadBalancerUuid(uuid());
        return event;
    }

}
