package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/8/2015.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveVmNicFromLoadBalancerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIRemoveVmNicFromLoadBalancerEvent() {
    }

    public APIRemoveVmNicFromLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveVmNicFromLoadBalancerEvent __example__() {
        APIRemoveVmNicFromLoadBalancerEvent event = new APIRemoveVmNicFromLoadBalancerEvent();
        LoadBalancerInventory lb = new LoadBalancerInventory();

        lb.setName("Test-Lb");
        lb.setVipUuid(uuid());
        lb.setUuid(uuid());

        event.setInventory(lb);
        return event;
    }

}
