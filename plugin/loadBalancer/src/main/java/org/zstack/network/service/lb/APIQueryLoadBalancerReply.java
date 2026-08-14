package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 8/18/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryLoadBalancerReply extends APIQueryReply {
    private List<LoadBalancerInventory> inventories;

    public List<LoadBalancerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryLoadBalancerReply __example__() {
        APIQueryLoadBalancerReply reply = new APIQueryLoadBalancerReply();
        LoadBalancerInventory lb = new LoadBalancerInventory();

        lb.setName("Test-Lb");
        lb.setVipUuid(uuid());
        lb.setUuid(uuid());

        reply.setInventories(Arrays.asList(lb));
        return reply;
    }

}
