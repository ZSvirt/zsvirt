package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.service.lb.LoadBalancerInventory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin.ruan 2021/03/23
 */
@RestResponse(allTo = "inventories")
public class APIGetVpcAttachedLoadBalancerReply extends APIReply {
    private List<LoadBalancerInventory> inventories;

    public List<LoadBalancerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcAttachedLoadBalancerReply __example__() {
        APIGetVpcAttachedLoadBalancerReply reply = new APIGetVpcAttachedLoadBalancerReply();

        LoadBalancerInventory inventory = new LoadBalancerInventory();
        inventory.setName("Test-Lb");
        inventory.setVipUuid(uuid());
        inventory.setUuid(uuid());

        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
