package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryLoadBalancerServerGroupReply extends APIQueryReply {
    private List<LoadBalancerServerGroupInventory> inventories;

    public List<LoadBalancerServerGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerServerGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryLoadBalancerServerGroupReply __example__() {
        APIQueryLoadBalancerServerGroupReply reply = new APIQueryLoadBalancerServerGroupReply();
        LoadBalancerServerGroupInventory inv = new LoadBalancerServerGroupInventory();

        inv.setName("Test-LbServerGroup");
        inv.setUuid(uuid());
        reply.setInventories(Arrays.asList(inv));
        return reply;
    }

}
