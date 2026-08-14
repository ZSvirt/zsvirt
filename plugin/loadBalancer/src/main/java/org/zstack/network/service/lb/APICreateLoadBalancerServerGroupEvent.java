package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
@RestResponse(allTo = "inventory")
public class APICreateLoadBalancerServerGroupEvent extends APIEvent{
    private LoadBalancerServerGroupInventory inventory;

    public APICreateLoadBalancerServerGroupEvent() {
    }

    public APICreateLoadBalancerServerGroupEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerServerGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerServerGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateLoadBalancerServerGroupEvent __example__() {
        APICreateLoadBalancerServerGroupEvent event = new APICreateLoadBalancerServerGroupEvent();
        LoadBalancerServerGroupInventory loadBalancerServerGroupIv = new LoadBalancerServerGroupInventory();
        loadBalancerServerGroupIv.setName("create-servergroup");
        loadBalancerServerGroupIv.setUuid(uuid());
        loadBalancerServerGroupIv.setLoadBalancerUuid(uuid());
        return event;
    }

}
