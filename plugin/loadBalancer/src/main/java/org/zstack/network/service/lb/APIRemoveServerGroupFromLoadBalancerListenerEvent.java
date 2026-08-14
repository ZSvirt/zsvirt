package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRemoveServerGroupFromLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIRemoveServerGroupFromLoadBalancerListenerEvent() {
    }

    public APIRemoveServerGroupFromLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public static APIRemoveServerGroupFromLoadBalancerListenerEvent __example__() {
        APIRemoveServerGroupFromLoadBalancerListenerEvent event = new APIRemoveServerGroupFromLoadBalancerListenerEvent();
        LoadBalancerListenerInventory Listenerinv = new LoadBalancerListenerInventory();
        Listenerinv.setUuid(uuid());
        Listenerinv.setLoadBalancerUuid(uuid());
        Listenerinv.setName("Test-Listener");
        Listenerinv.setLoadBalancerPort(80);
        Listenerinv.setInstancePort(80);
        Listenerinv.setProtocol(LoadBalancerConstants.LB_PROTOCOL_HTTP);

        event.setInventory(Listenerinv);
        return event;
    }
}
