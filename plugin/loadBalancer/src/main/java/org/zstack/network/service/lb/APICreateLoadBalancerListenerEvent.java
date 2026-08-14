package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/8/2015.
 */
@RestResponse(allTo = "inventory")
public class APICreateLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APICreateLoadBalancerListenerEvent() {
    }

    public APICreateLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }
 
    public static APICreateLoadBalancerListenerEvent __example__() {
        APICreateLoadBalancerListenerEvent event = new APICreateLoadBalancerListenerEvent();
        LoadBalancerListenerInventory l = new LoadBalancerListenerInventory();

        l.setUuid(uuid());
        l.setLoadBalancerUuid(uuid());
        l.setName("Test-Listener");
        l.setLoadBalancerPort(80);
        l.setInstancePort(80);
        l.setProtocol(LoadBalancerConstants.LB_PROTOCOL_HTTP);

        event.setInventory(l);
        return event;
    }

}
