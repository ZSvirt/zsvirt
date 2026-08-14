package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeLoadBalancerBackendServerEvent extends APIEvent {
    private LoadBalancerServerGroupInventory inventory;

    public APIChangeLoadBalancerBackendServerEvent() {
    }

    public APIChangeLoadBalancerBackendServerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerServerGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerServerGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeLoadBalancerBackendServerEvent __example__() {
        APIChangeLoadBalancerBackendServerEvent event = new APIChangeLoadBalancerBackendServerEvent();
        LoadBalancerServerGroupInventory loadBalancerServerGroupIv = new LoadBalancerServerGroupInventory();
        loadBalancerServerGroupIv.setName("servergroup");
        loadBalancerServerGroupIv.setUuid(uuid());
        loadBalancerServerGroupIv.setLoadBalancerUuid(uuid());
        return event;
    }
}
