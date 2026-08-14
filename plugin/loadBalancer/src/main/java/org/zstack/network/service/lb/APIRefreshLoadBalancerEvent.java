package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/18/2015.
 */
@RestResponse(allTo = "inventory")
public class APIRefreshLoadBalancerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIRefreshLoadBalancerEvent() {
    }

    public APIRefreshLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRefreshLoadBalancerEvent __example__() {
        APIRefreshLoadBalancerEvent event = new APIRefreshLoadBalancerEvent();
        LoadBalancerInventory lb = new LoadBalancerInventory();

        lb.setName("Test-Lb");
        lb.setVipUuid(uuid());
        lb.setUuid(uuid());

        event.setInventory(lb);
        return event;
    }

}
