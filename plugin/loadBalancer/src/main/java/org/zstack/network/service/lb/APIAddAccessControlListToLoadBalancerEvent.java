package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@RestResponse(allTo = "inventory")
public class APIAddAccessControlListToLoadBalancerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIAddAccessControlListToLoadBalancerEvent() {
    }

    public APIAddAccessControlListToLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddAccessControlListToLoadBalancerEvent __example__() {
        APIAddAccessControlListToLoadBalancerEvent event = new APIAddAccessControlListToLoadBalancerEvent();
        LoadBalancerListenerInventory listener = new LoadBalancerListenerInventory();
        LoadBalancerListenerACLRefInventory ref = new LoadBalancerListenerACLRefInventory();

        String listenerUuid = uuid();

        ref.setListenerUuid(listenerUuid);
        ref.setAclUuid(uuid());
        ref.setType(LoadBalancerAclType.black.toString());

        listener.setLoadBalancerUuid(listenerUuid);
        listener.setName("Test-Lb-Listener");
        listener.setAclRefs(Arrays.asList(ref));

        return event;
    }

}