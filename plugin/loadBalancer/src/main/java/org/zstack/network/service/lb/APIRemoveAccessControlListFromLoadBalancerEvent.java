package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@RestResponse(allTo = "inventory")
public class APIRemoveAccessControlListFromLoadBalancerEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIRemoveAccessControlListFromLoadBalancerEvent() {
    }

    public APIRemoveAccessControlListFromLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRemoveAccessControlListFromLoadBalancerEvent __example__() {
        APIRemoveAccessControlListFromLoadBalancerEvent event = new APIRemoveAccessControlListFromLoadBalancerEvent();
        LoadBalancerListenerInventory lbl = new LoadBalancerListenerInventory();

        lbl.setName("test-lb-listener");
        lbl.setUuid(uuid());

        event.setInventory(lbl);
        return event;
    }
}
