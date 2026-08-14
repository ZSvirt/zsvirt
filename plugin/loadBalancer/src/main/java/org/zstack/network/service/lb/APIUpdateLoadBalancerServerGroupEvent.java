package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateLoadBalancerServerGroupEvent extends APIEvent {
    private LoadBalancerServerGroupInventory inventory;

    public APIUpdateLoadBalancerServerGroupEvent() {
    }

    public APIUpdateLoadBalancerServerGroupEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerServerGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerServerGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateLoadBalancerServerGroupEvent __example__() {
        APIUpdateLoadBalancerServerGroupEvent event = new APIUpdateLoadBalancerServerGroupEvent();
        LoadBalancerServerGroupInventory loadBalancerServerGroupIv = new LoadBalancerServerGroupInventory();
        loadBalancerServerGroupIv.setName("update-servergroup");
        loadBalancerServerGroupIv.setUuid(uuid());
        loadBalancerServerGroupIv.setLoadBalancerUuid(uuid());
        return event;
    }
}
