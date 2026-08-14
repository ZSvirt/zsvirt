package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin.ruan on 02/23/2019.
 */
@RestResponse(allTo = "inventory")
public class APIChangeLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIChangeLoadBalancerListenerEvent() {
    }

    public APIChangeLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }
 
    public static APIChangeLoadBalancerListenerEvent __example__() {
        APIChangeLoadBalancerListenerEvent event = new APIChangeLoadBalancerListenerEvent();
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
