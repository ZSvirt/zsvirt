package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/8/2015.
 */
@RestResponse(allTo = "inventory")
public class APIDeleteLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIDeleteLoadBalancerListenerEvent() {
    }

    public APIDeleteLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDeleteLoadBalancerListenerEvent __example__() {
        APIDeleteLoadBalancerListenerEvent event = new APIDeleteLoadBalancerListenerEvent();
        event.setSuccess(true);
        return event;
    }

}
