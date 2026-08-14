package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Created by frank on 8/8/2015.
 */
@RestResponse(allTo = "inventory")
public class APIAddVmNicToLoadBalancerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIAddVmNicToLoadBalancerEvent() {
    }

    public APIAddVmNicToLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddVmNicToLoadBalancerEvent __example__() {
        APIAddVmNicToLoadBalancerEvent event = new APIAddVmNicToLoadBalancerEvent();
        LoadBalancerListenerInventory listener = new LoadBalancerListenerInventory();
        LoadBalancerListenerVmNicRefInventory lvnr = new LoadBalancerListenerVmNicRefInventory();

        String listenerUuid = uuid();

        lvnr.setListenerUuid(listenerUuid);
        lvnr.setVmNicUuid(uuid());

        listener.setLoadBalancerUuid(listenerUuid);
        listener.setName("Test-Lb-Listener");
        listener.setVmNicRefs(Arrays.asList(lvnr));

        return event;
    }

}
