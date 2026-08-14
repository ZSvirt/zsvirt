package org.zstack.network.service.lb;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 8/18/2015.
 */
public class RefreshLoadBalancerReply extends MessageReply {
    private LoadBalancerInventory inventory;

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
}
