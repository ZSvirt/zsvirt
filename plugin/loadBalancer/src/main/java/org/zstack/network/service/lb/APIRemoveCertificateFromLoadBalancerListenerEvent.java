package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Created by shixin on 03/22/2018.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveCertificateFromLoadBalancerListenerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIRemoveCertificateFromLoadBalancerListenerEvent() {
    }

    public APIRemoveCertificateFromLoadBalancerListenerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveCertificateFromLoadBalancerListenerEvent __example__() {
        APIRemoveCertificateFromLoadBalancerListenerEvent event = new APIRemoveCertificateFromLoadBalancerListenerEvent();
        LoadBalancerListenerInventory listener = new LoadBalancerListenerInventory();
        LoadBalancerListenerVmNicRefInventory lvnr = new LoadBalancerListenerVmNicRefInventory();

        String listenerUuid = uuid();

        lvnr.setListenerUuid(listenerUuid);
        lvnr.setVmNicUuid(uuid());

        listener.setLoadBalancerUuid(listenerUuid);
        listener.setName("Test-Lb-Listener");
        listener.setVmNicRefs(Arrays.asList(lvnr));

        event.setInventory(listener);

        return event;
    }

}
