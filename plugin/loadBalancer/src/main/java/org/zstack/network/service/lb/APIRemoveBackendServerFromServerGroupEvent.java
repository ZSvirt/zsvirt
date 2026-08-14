package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRemoveBackendServerFromServerGroupEvent  extends APIEvent {
    private LoadBalancerServerGroupInventory inventory;

    public APIRemoveBackendServerFromServerGroupEvent() {
    }

    public APIRemoveBackendServerFromServerGroupEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerServerGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerServerGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRemoveBackendServerFromServerGroupEvent __example__() {
        APIRemoveBackendServerFromServerGroupEvent event = new APIRemoveBackendServerFromServerGroupEvent();
        LoadBalancerServerGroupInventory loadBalancerServerGroupIv = new LoadBalancerServerGroupInventory();
        loadBalancerServerGroupIv.setName("test-servergroup");
        loadBalancerServerGroupIv.setUuid(uuid());
        loadBalancerServerGroupIv.setLoadBalancerUuid(uuid());
        return event;
    }

}
