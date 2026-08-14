package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 8/18/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryLoadBalancerListenerReply extends APIQueryReply {
    private List<LoadBalancerListenerInventory> inventories;

    public List<LoadBalancerListenerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerListenerInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryLoadBalancerListenerReply __example__() {
        APIQueryLoadBalancerListenerReply reply = new APIQueryLoadBalancerListenerReply();
        LoadBalancerListenerInventory l = new LoadBalancerListenerInventory();

        l.setUuid(uuid());
        l.setLoadBalancerUuid(uuid());
        l.setName("Test-Listener");
        l.setLoadBalancerPort(80);
        l.setInstancePort(80);
        l.setProtocol(LoadBalancerConstants.LB_PROTOCOL_HTTP);

        reply.setInventories(Arrays.asList(l));
        return reply;
    }

}
