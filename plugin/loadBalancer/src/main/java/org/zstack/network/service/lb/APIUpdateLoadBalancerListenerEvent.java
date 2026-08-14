package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by camile on 5/19/2017.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIUpdateLoadBalancerListenerEvent() {
    }

    public APIUpdateLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }
 
    public static APIUpdateLoadBalancerListenerEvent __example__() {
        APIUpdateLoadBalancerListenerEvent event = new APIUpdateLoadBalancerListenerEvent();
        LoadBalancerListenerInventory l = new LoadBalancerListenerInventory();

        l.setUuid(uuid());
        l.setLoadBalancerUuid(uuid());
        l.setName("Test-Listener");
        l.setDescription("desc info");
        l.setLoadBalancerPort(80);
        l.setInstancePort(80);
        l.setProtocol(LoadBalancerConstants.LB_PROTOCOL_HTTP);

        event.setInventory(l);
        return event;
    }

}
