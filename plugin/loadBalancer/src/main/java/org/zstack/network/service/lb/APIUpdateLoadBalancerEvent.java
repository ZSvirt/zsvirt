package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by camile on 5/18/20157
 */
@RestResponse(allTo = "inventory")
public class APIUpdateLoadBalancerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIUpdateLoadBalancerEvent() {
    }

    public APIUpdateLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateLoadBalancerEvent __example__() {
        APIUpdateLoadBalancerEvent event = new APIUpdateLoadBalancerEvent();
        LoadBalancerInventory lb = new LoadBalancerInventory();

        lb.setName("Test-Lb");
        lb.setVipUuid(uuid());
        lb.setUuid(uuid());

        return event;
    }

}
